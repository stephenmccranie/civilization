"""Draw the proposed whole-percent equipment-grade distribution."""

from __future__ import annotations

import argparse
from math import erf, sqrt
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--mean", type=float, default=75.0)
    parser.add_argument("--sd", type=float, default=10.0)
    parser.add_argument("--output", type=Path, default=Path("concept_art/equipment_grade_distribution.png"))
    args = parser.parse_args()
    if args.sd <= 0:
        parser.error("--sd must be positive")

    def cdf(value: float) -> float:
        return (1 + erf((value - args.mean) / (args.sd * sqrt(2)))) / 2

    def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
        name = "segoeuib.ttf" if bold else "segoeui.ttf"
        return ImageFont.truetype(str(Path("C:/Windows/Fonts") / name), size)

    image = Image.new("RGB", (1500, 840), "#ece6d9")
    draw = ImageDraw.Draw(image)
    ink, muted = "#18292d", "#526064"
    draw.text((95, 47), "Equipment grade: proposed chance curve", font=font(45, True), fill=ink)
    draw.text((98, 113), f"Normal roll, center {args.mean:g}%, standard deviation {args.sd:g} points; rounded to whole percent", font=font(25), fill=muted)

    left, top, right, bottom = 100, 210, 1410, 666
    grades = range(35, 121)
    probabilities = {grade: cdf(grade + .5) - cdf(grade - .5) for grade in grades}
    ymax = max(probabilities.values()) * 1.18
    for tick in range(5):
        chance = tick / 100
        y = round(bottom - chance / ymax * (bottom - top))
        if y < top:
            continue
        draw.line((left, y, right, y), fill="#c9c8bd", width=2)
        draw.text((31, y - 17), f"{tick}%", font=font(21), fill=muted)

    width = (right - left) / len(grades)
    for grade in grades:
        x0 = round(left + (grade - grades.start) * width + 1)
        x1 = round(left + (grade - grades.start + 1) * width - 1)
        y = round(bottom - probabilities[grade] / ymax * (bottom - top))
        color = "#ae674b" if grade < 50 else "#557882" if grade < 100 else "#c19139" if grade == 100 else "#336e63"
        draw.rectangle((x0, y, x1, bottom), fill=color)

    draw.line((left, bottom, right, bottom), fill=ink, width=3)
    for grade in range(40, 121, 10):
        x = round(left + (grade - grades.start + .5) * width)
        draw.line((x, bottom, x, bottom + 10), fill=ink, width=2)
        draw.text((x - 20, bottom + 15), str(grade), font=font(21), fill=ink)
    draw.text((left + 495, bottom + 60), "Finished equipment grade (%)", font=font(25, True), fill=ink)

    rare_low = cdf(49.5) * 100
    exact_hundred = (cdf(100.5) - cdf(99.5)) * 100
    above_hundred = (1 - cdf(100.5)) * 100
    callouts = [
        ("Below 50%", f"{rare_low:.2f}%", "#ae674b"),
        ("Exactly 100%", f"{exact_hundred:.2f}%", "#c19139"),
        ("Above 100%", f"{above_hundred:.2f}%", "#336e63"),
    ]
    for i, (label, value, color) in enumerate(callouts):
        x = 200 + i * 450
        draw.rectangle((x, 762, x + 19, 781), fill=color)
        draw.text((x + 31, 755), f"{label}: {value}", font=font(23, True), fill=ink)

    args.output.parent.mkdir(parents=True, exist_ok=True)
    image.save(args.output)
    print(args.output.resolve())


if __name__ == "__main__":
    main()
