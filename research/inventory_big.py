#!/usr/bin/env python3
"""Read-only archive inventory for FIFA 14 Android distributables.

This program never writes to source APK, OBB, or XAPK files. It writes a JSON
manifest, a CSV of parsed BIG members, and optional extracted working copies
under the provided analysis directory.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
import os
import shutil
import struct
import sys
import zipfile
from datetime import datetime, timezone
from pathlib import Path, PurePosixPath
from typing import Any


BIG_SIGNATURES = {b"BIGF", b"BIG4", b"BIGH"}
ARCHIVE_EXTENSIONS = {".apk", ".xapk", ".obb", ".zip"}


def sha256_file(path: Path, chunk_size: int = 1024 * 1024) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        while chunk := handle.read(chunk_size):
            digest.update(chunk)
    return digest.hexdigest()


def safe_target(root: Path, member_name: str) -> Path:
    relative = PurePosixPath(member_name)
    if relative.is_absolute() or ".." in relative.parts:
        raise ValueError(f"Unsafe archive member name: {member_name!r}")
    target = root.joinpath(*relative.parts)
    target.parent.mkdir(parents=True, exist_ok=True)
    return target


def is_zip(path: Path) -> bool:
    try:
        return zipfile.is_zipfile(path)
    except OSError:
        return False


def list_zip(path: Path) -> list[dict[str, Any]]:
    with zipfile.ZipFile(path) as archive:
        return [
            {
                "name": item.filename,
                "uncompressed_size": item.file_size,
                "compressed_size": item.compress_size,
                "crc32": f"{item.CRC:08x}",
            }
            for item in archive.infolist()
        ]


def extract_members(path: Path, destination: Path, predicate) -> list[Path]:
    extracted: list[Path] = []
    with zipfile.ZipFile(path) as archive:
        for item in archive.infolist():
            if item.is_dir() or not predicate(item.filename):
                continue
            target = safe_target(destination, item.filename)
            with archive.open(item) as source, target.open("wb") as output:
                shutil.copyfileobj(source, output, length=1024 * 1024)
            extracted.append(target)
    return extracted


def read_cstring(handle, limit: int = 4096) -> str:
    raw = bytearray()
    for _ in range(limit):
        byte = handle.read(1)
        if not byte or byte == b"\0":
            break
        raw.extend(byte)
    return raw.decode("latin-1", errors="replace")


def parse_bigf(path: Path) -> dict[str, Any]:
    """Parse standard BIGF/BIG4/BIGH members without changing the archive."""
    size = path.stat().st_size
    with path.open("rb") as handle:
        signature = handle.read(4)
        if signature not in BIG_SIGNATURES:
            return {
                "format": "unrecognized",
                "signature_hex": signature.hex(),
                "size_bytes": size,
                "members": [],
            }
        archive_size_le = struct.unpack("<I", handle.read(4))[0]
        file_count = struct.unpack(">I", handle.read(4))[0]
        header_size = struct.unpack(">I", handle.read(4))[0]
        if file_count > 500000 or header_size > size or header_size < 16:
            return {
                "format": "invalid_or_nonstandard_big",
                "signature": signature.decode("ascii", errors="replace"),
                "archive_size_le": archive_size_le,
                "declared_file_count": file_count,
                "declared_header_size": header_size,
                "size_bytes": size,
                "members": [],
            }
        members: list[dict[str, Any]] = []
        for index in range(file_count):
            if handle.tell() + 8 > header_size:
                break
            offset = struct.unpack(">I", handle.read(4))[0]
            member_size = struct.unpack(">I", handle.read(4))[0]
            name = read_cstring(handle)
            members.append(
                {
                    "index": index,
                    "name": name,
                    "offset": offset,
                    "size_bytes": member_size,
                    "range_valid": offset + member_size <= size,
                }
            )
        return {
            "format": signature.decode("ascii", errors="replace"),
            "archive_size_le": archive_size_le,
            "declared_file_count": file_count,
            "declared_header_size": header_size,
            "size_bytes": size,
            "parsed_member_count": len(members),
            "members": members,
        }


def classify_path(name: str) -> list[str]:
    lowered = name.casefold()
    labels: list[str] = []
    groups = {
        "menu": ("menu", "frontend", "front_end", "mainmenu", "home"),
        "hud": ("hud", "overlay", "ingame", "in_game"),
        "scoreboard": ("scoreboard", "score", "clock", "timer", "full_time", "halftime"),
        "background": ("background", "backdrop", "bg_", "splash", "loading"),
        "button": ("button", "btn", "selector", "tab"),
        "icon": ("icon", "glyph", "crest", "badge"),
        "ui_layout": (".apt", ".const", ".xml", ".layout"),
        "ui_image": (".tga", ".png", ".dds", ".fsh", ".rx2"),
    }
    for label, terms in groups.items():
        if any(term in lowered for term in terms):
            labels.append(label)
    return labels


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--originals", type=Path, required=True)
    parser.add_argument("--analysis", type=Path, required=True)
    parser.add_argument("--extract-big", action="store_true", help="Extract .big working copies only")
    args = parser.parse_args()

    originals = args.originals.resolve()
    analysis = args.analysis.resolve()
    staged = analysis / "extracted" / "top_level"
    big_root = analysis / "extracted" / "big"
    analysis.mkdir(parents=True, exist_ok=True)

    sources = sorted(
        item for item in originals.rglob("*") if item.is_file() and item.suffix.casefold() in ARCHIVE_EXTENSIONS
    )
    manifest: dict[str, Any] = {
        "generated_at_utc": datetime.now(timezone.utc).isoformat(),
        "originals_directory": str(originals),
        "read_only": True,
        "sources": [],
        "big_files": [],
    }

    for source in sources:
        record: dict[str, Any] = {
            "path": str(source.relative_to(originals)),
            "size_bytes": source.stat().st_size,
            "sha256": sha256_file(source),
            "is_zip": is_zip(source),
        }
        if record["is_zip"]:
            record["members"] = list_zip(source)
            # Extract nested OBB/APK only as analysis working copies.
            if source.suffix.casefold() == ".xapk":
                extracted = extract_members(
                    source,
                    staged / source.stem,
                    lambda name: Path(name).suffix.casefold() in {".obb", ".apk"},
                )
                record["extracted_working_copies"] = [str(item.relative_to(analysis)) for item in extracted]
        manifest["sources"].append(record)

    candidate_files = [item for item in staged.rglob("*") if item.is_file()]
    candidate_files.extend(item for item in originals.rglob("*") if item.is_file() and item.suffix.casefold() == ".obb")
    for candidate in candidate_files:
        if not is_zip(candidate):
            continue
        container_label = candidate.relative_to(analysis).as_posix() if candidate.is_relative_to(analysis) else candidate.name
        members = list_zip(candidate)
        container_record = {
            "path": container_label,
            "size_bytes": candidate.stat().st_size,
            "sha256": sha256_file(candidate),
            "is_zip": True,
            "members": members,
        }
        manifest["sources"].append(container_record)
        if args.extract_big:
            extract_members(candidate, big_root / candidate.stem, lambda name: name.casefold().endswith(".big"))

    for big_file in sorted(big_root.rglob("*.big")):
        parsed = parse_bigf(big_file)
        parsed["path"] = str(big_file.relative_to(analysis))
        parsed["sha256"] = sha256_file(big_file)
        manifest["big_files"].append(parsed)

    json_path = analysis / "manifest.json"
    json_path.write_text(json.dumps(manifest, indent=2, ensure_ascii=False), encoding="utf-8")
    csv_path = analysis / "big_members.csv"
    with csv_path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(
            handle,
            fieldnames=["big_file", "format", "index", "member_name", "offset", "size_bytes", "range_valid", "classification"],
        )
        writer.writeheader()
        for big_file in manifest["big_files"]:
            for member in big_file.get("members", []):
                writer.writerow(
                    {
                        "big_file": big_file["path"],
                        "format": big_file["format"],
                        "index": member["index"],
                        "member_name": member["name"],
                        "offset": member["offset"],
                        "size_bytes": member["size_bytes"],
                        "range_valid": member["range_valid"],
                        "classification": ";".join(classify_path(member["name"])),
                    }
                )
    print(f"Manifest: {json_path}")
    print(f"BIG member table: {csv_path}")
    print(f"BIG files parsed: {len(manifest['big_files'])}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
