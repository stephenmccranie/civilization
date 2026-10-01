"""Render the visual-only operating view from the exact v3 structural plan."""

from pathlib import Path
import runpy

PLAN = runpy.run_path(str(Path(__file__).with_name("oil_block_plan_v3.py")))["OPERATING_PLAN"]
