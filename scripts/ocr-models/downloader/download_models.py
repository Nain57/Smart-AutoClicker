# Copyright (C) 2026 Kevin Buzeau
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#   *
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

import os
import yaml
import tarfile
import requests
import argparse


# ----------------------------
# Default YAML path
# ----------------------------

def get_default_yaml():
    return os.path.join(
        os.path.dirname(os.path.abspath(__file__)),
        "models.yaml"
    )

# Removes "._" files
def cleanup_macos_artifacts(directory):
    print(f"[CLEANUP] Removing macOS artifacts in {directory}")

    removed = 0

    for root, _, files in os.walk(directory):
        for f in files:
            if f.startswith("._") or f == ".DS_Store":
                path = os.path.join(root, f)
                try:
                    os.remove(path)
                    removed += 1
                except Exception as e:
                    print(f"[WARN] Could not remove {path}: {e}")

    if removed:
        print(f"[CLEANUP] Removed {removed} artifact files")

# ----------------------------
# Download helper
# ----------------------------

def download_file(url, dest):
    if url is None:
        return

    if os.path.exists(dest):
        print(f"[SKIP] {dest}")
        return

    print(f"[DOWNLOAD] {url}")

    r = requests.get(url, stream=True)
    r.raise_for_status()

    os.makedirs(os.path.dirname(dest), exist_ok=True)

    with open(dest, "wb") as f:
        for chunk in r.iter_content(chunk_size=8192):
            if chunk:
                f.write(chunk)


# ----------------------------
# Extract + cleanup
# ----------------------------

def extract_tar_and_cleanup(tar_path, output_dir):
    print(f"[EXTRACT] {tar_path}")

    with tarfile.open(tar_path, "r:*") as tar:
        members = tar.getmembers()

        for member in members:
            if member.isdir():
                continue

            filename = os.path.basename(member.name)
            target_path = os.path.join(output_dir, filename)

            # skip weird hidden files immediately
            if filename.startswith("._") or filename == ".DS_Store":
                continue

            with tar.extractfile(member) as src, open(target_path, "wb") as dst:
                dst.write(src.read())

    print(f"[CLEANUP] removing {tar_path}")
    os.remove(tar_path)

    # 🔥 NEW STEP: remove macOS artifacts just in case
    cleanup_macos_artifacts(output_dir)


# ----------------------------
# DET models (flattened)
# ----------------------------

def process_det(model, output_root):
    name = model["name"]
    inference_url = model.get("inference_url")

    # FLAT STRUCTURE: output/det/
    out_dir = os.path.join(output_root, "det")
    os.makedirs(out_dir, exist_ok=True)

    archive_path = os.path.join(out_dir, f"{name}.tar")

    download_file(inference_url, archive_path)
    extract_tar_and_cleanup(archive_path, out_dir)


# ----------------------------
# REC models (flattened by alphabet)
# ----------------------------

def process_rec(model, output_root):
    alphabet = model.get("alphabet", "unknown")
    inference_url = model.get("inference_url")
    dict_url = model.get("dict_url")

    # FLAT STRUCTURE: output/rec/{alphabet}/
    out_dir = os.path.join(output_root, "rec", alphabet)
    os.makedirs(out_dir, exist_ok=True)

    archive_path = os.path.join(out_dir, f"{alphabet}.tar")

    download_file(inference_url, archive_path)
    extract_tar_and_cleanup(archive_path, out_dir)

    # ----------------------------
    # Dictionary file
    # ----------------------------
    if dict_url:
        dict_name = "dict.txt"
        dict_path = os.path.join(out_dir, dict_name)

        download_file(dict_url, dict_path)


# ----------------------------
# Dispatcher
# ----------------------------

def process_model(model, output_root):
    mtype = model["type"]

    if mtype == "det":
        process_det(model, output_root)

    elif mtype == "rec":
        process_rec(model, output_root)

    else:
        print(f"[WARN] Unknown model type: {mtype}")


# ----------------------------
# MAIN
# ----------------------------

def main_logic(yaml_path, output_root, mode="all"):
    yaml_path = os.path.abspath(yaml_path)
    output_root = os.path.abspath(output_root)

    print("\n=== PaddleOCR Model Downloader (Clean Version) ===")
    print("[INFO] YAML:", yaml_path)
    print("[INFO] OUTPUT:", output_root)
    print("[INFO] MODE:", mode)

    with open(yaml_path, "r", encoding="utf-8") as f:
        config = yaml.safe_load(f)

    models = config.get("models", [])

    # Filter models based on mode
    if mode == "default":
        models = [m for m in models if m.get("default") == True]
    elif mode == "language_pack":
        models = [m for m in models if m.get("default") != True]

    for model in models:
        print("\n-----------------------------------")
        print(f"[MODEL] {model['name']}")
        process_model(model, output_root)

def main():
    parser = argparse.ArgumentParser()

    parser.add_argument(
        "--input",
        default=get_default_yaml(),
        help="Path to models.yaml"
    )

    parser.add_argument(
        "--output",
        required=True,
        help="Output directory"
    )

    parser.add_argument(
        "--mode",
        choices=["all", "default", "language_pack"],
        default="all",
        help="Download mode: 'default' (detection + latin), 'language_pack' (all others), or 'all' (default)"
    )

    args = parser.parse_args()

    main_logic(args.input, args.output, args.mode)

    print("\n[DONE] All models processed")


if __name__ == "__main__":
    main()