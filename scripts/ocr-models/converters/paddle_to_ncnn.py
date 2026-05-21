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

"""
Converts a PaddleOCR model folder to NCNN format.
This script orchestrates the full conversion pipeline:
Paddle -> ONNX -> ONNX-Simplify -> NCNN (PNNX).
"""

import os
import shutil
import argparse
import sys

# Import local converter modules
import paddle_to_onnx
import onnx_simplify
import onnx_to_ncnn

def run_conversion(model_dir, model_type, fp16=True):
    """
    Executes the full conversion pipeline for a single model folder.

    :param model_dir: Path to the directory containing PaddlePaddle model files.
    :param model_type: Type of model: "det" (detection) or "rec" (recognition).
    :param fp16: Whether to use FP16 precision for NCNN conversion.
    """
    model_dir = os.path.abspath(model_dir)
    if not os.path.isdir(model_dir):
        print(f"[ERROR] Input directory does not exist: {model_dir}")
        sys.exit(1)

    # 1. Setup temporary workspace
    temp_dir = os.path.join(model_dir, "temp_conv")
    if os.path.exists(temp_dir):
        shutil.rmtree(temp_dir)
    os.makedirs(temp_dir)

    onnx_path = os.path.join(temp_dir, "model.onnx")
    sim_onnx_path = os.path.join(temp_dir, "model_sim.onnx")
    ncnn_work_dir = os.path.join(temp_dir, "ncnn_out")
    os.makedirs(ncnn_work_dir)

    try:
        # 2. Step 1: Paddle to ONNX
        print(f"\n--- [1/3] Exporting Paddle to ONNX ---")
        paddle_to_onnx.main_logic(model_dir, onnx_path)

        # 3. Step 2: ONNX Simplification
        # Detection: No shape set. Recognition: [1,3,48,320]
        shape = "1,3,48,320" if model_type == "rec" else None
        print(f"\n--- [2/3] Simplifying ONNX (Shape: {shape}) ---")
        onnx_simplify.simplify_onnx(onnx_path, sim_onnx_path, shape)

        # 4. Step 3: ONNX to NCNN (PNNX)
        print(f"\n--- [3/3] Converting to NCNN ---")
        onnx_to_ncnn.convert_pnnx(sim_onnx_path, ncnn_work_dir, shape, fp16=fp16)

        # 5. Finalizing: Move NCNN files to the model_dir
        print(f"\n--- Finalizing output ---")

        # Look for the generated ncnn files.
        # PNNX may place them in temp_dir if absolute paths are used.
        param_file = None
        bin_file = None

        # Search both the work dir and the temp dir
        search_dirs = [ncnn_work_dir, temp_dir]
        for s_dir in search_dirs:
            for f in os.listdir(s_dir):
                if f.endswith(".ncnn.param"):
                    param_file = os.path.join(s_dir, f)
                elif f.endswith(".ncnn.bin"):
                    bin_file = os.path.join(s_dir, f)

        if not param_file or not bin_file:
            print("[ERROR] NCNN conversion failed to produce output files.")
            sys.exit(1)

        # Target names: det.ncnn.param/bin or rec.ncnn.param/bin
        final_param = os.path.join(model_dir, f"{model_type}.ncnn.param")
        final_bin = os.path.join(model_dir, f"{model_type}.ncnn.bin")

        shutil.move(param_file, final_param)
        shutil.move(bin_file, final_bin)

        print(f"[SUCCESS] NCNN model created in {model_dir}:")
        print(f"  - {os.path.basename(final_param)}")
        print(f"  - {os.path.basename(final_bin)}")

    finally:
        # 6. Cleanup
        if os.path.exists(temp_dir):
            shutil.rmtree(temp_dir)

def main():
    parser = argparse.ArgumentParser(description="Convert a PaddleOCR model folder to NCNN.")
    parser.add_argument("--input", required=True, help="Path to Paddle model folder")
    parser.add_argument("--type", required=True, choices=["det", "rec"], help="Model type: 'det' or 'rec'")
    parser.add_argument("--fp16", action="store_true", default=True, help="Enable FP16 mode (default: True)")
    parser.add_argument("--no-fp16", action="store_false", dest="fp16", help="Disable FP16 mode")

    args = parser.parse_args()

    run_conversion(args.input, args.type, args.fp16)

if __name__ == "__main__":
    main()
