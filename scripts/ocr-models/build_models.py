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
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

import os
import shutil
import argparse
import sys
import yaml

# Add subdirectories to path so we can import the scripts as modules
sys.path.append(os.path.join(os.path.dirname(__file__), "downloader"))
sys.path.append(os.path.join(os.path.dirname(__file__), "converters"))

import download_models
import paddle_to_ncnn
import dependency_checker

# ----------------------------
# Model Processor
# ----------------------------

def process_model(model, output_root):
    name = model["name"]
    mtype = model["type"]
    alphabet = model.get("alphabet", "all")
    
    # Determine the directory where download_models.py placed the files
    if mtype == "det":
        model_dir = os.path.join(output_root, "det")
    else:
        model_dir = os.path.join(output_root, "rec", alphabet)

    print(f"\n\n========================================")
    print(f"PROCESSING: {name} ({mtype})")
    print(f"========================================")

    # 1. Convert using the unified converter
    # This handles Paddle -> ONNX -> Simplify -> NCNN and moves files to model_dir
    paddle_to_ncnn.run_conversion(model_dir, mtype)

    # 2. Cleanup: Remove original downloaded files, keep only NCNN and dictionary
    print(f"\n>>> [STEP] Cleaning up original files in {model_dir}")
    
    ncnn_extensions = [".ncnn.param", ".ncnn.bin"]
    allowed_files = ["dict.txt"]

    for f in os.listdir(model_dir):
        file_path = os.path.join(model_dir, f)
        
        # Keep dictionary and the newly generated NCNN files
        if f in allowed_files or any(f.endswith(ext) for ext in ncnn_extensions):
            continue

        if os.path.isfile(file_path):
            os.remove(file_path)
        elif os.path.isdir(file_path):
            shutil.rmtree(file_path)


# ----------------------------
# MAIN
# ----------------------------

def main():
    dependency_checker.check_dependencies()

    parser = argparse.ArgumentParser(description="Full OCR Model Pipeline: Download -> ONNX -> NCNN")
    parser.add_argument("--output", required=True, help="Root output directory for models")
    parser.add_argument("--config", default=None, help="Path to models.yaml")
    parser.add_argument(
        "--mode",
        choices=["all", "default", "language_pack"],
        default="all",
        help="Build mode: 'default' (detection + latin), 'language_pack' (all others), or 'all' (default)"
    )

    args = parser.parse_args()

    output_root = os.path.abspath(args.output)
    yaml_path = args.config if args.config else os.path.join(os.path.dirname(__file__), "downloader", "models.yaml")

    # 1. Download all models
    print("\n=== STEP 1: Downloading Models ===")
    download_models.main_logic(yaml_path, output_root, args.mode)

    # 2. Load config to iterate
    with open(yaml_path, "r", encoding="utf-8") as f:
        config = yaml.safe_load(f)
    models = config.get("models", [])

    # Filter models based on mode
    if args.mode == "default":
        models = [m for m in models if m.get("default") == True]
    elif args.mode == "language_pack":
        models = [m for m in models if m.get("default") != True]

    # 3. Process each model
    for model in models:
        process_model(model, output_root)

    print("\n\n[SUCCESS] All models downloaded and converted to NCNN.")

if __name__ == "__main__":
    main()
