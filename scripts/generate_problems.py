#!/usr/bin/env python3
"""
Generates 400 Korean-English writing practice problems (100 per level)
using Claude API and saves them as JSON asset files.

Usage:
    export ANTHROPIC_API_KEY=your_key
    python3 scripts/generate_problems.py
"""

import anthropic
import json
import os
import sys
import time
from pathlib import Path

OUTPUT_DIR = Path(__file__).parent.parent / "app/src/main/assets/problems"

LEVEL_SPECS = {
    1: {
        "description": "단일 한국어 문장 → 영어 문장 1개",
        "topics": [
            "일상 인사와 소개", "날씨와 계절", "음식과 식사", "쇼핑과 가격",
            "교통과 이동", "가족과 관계", "직업과 일", "취미와 여가",
            "건강과 운동", "감정과 기분"
        ]
    },
    2: {
        "description": "하나의 주제와 관련된 한국어 문장 2개 → 영어 문장 2개",
        "topics": [
            "여행 계획", "음식 주문", "길 안내", "약속 잡기",
            "쇼핑 대화", "날씨 대화", "직장 생활", "건강 상담",
            "친구와 대화", "취미 설명"
        ]
    },
    3: {
        "description": "하나의 주제와 관련된 한국어 문장 3개 → 영어 문장 3개",
        "topics": [
            "여행 후기", "음식점 리뷰", "영화/책 감상", "환경 문제",
            "기술과 스마트폰", "교육과 학습", "건강한 생활습관", "직장 경험",
            "소셜 미디어", "경제와 소비"
        ]
    },
    4: {
        "description": "하나의 주제를 다루는 한국어 문단 (4~6문장) → 영어 문단",
        "topics": [
            "나의 일상 소개", "좋아하는 계절", "한국 음식 소개", "여행 경험",
            "직업 소개", "환경 보호의 중요성", "기술의 발전", "건강한 생활",
            "독서의 즐거움", "친구와 우정"
        ]
    }
}

GENERATE_PROMPT = """You are an English writing practice content creator for Korean learners.
Generate {count} Korean-to-English practice problems for Level {level}.

Level {level} specification: {description}

Topics to cover (generate problems spread across these topics):
{topics}

Return ONLY a valid JSON array with exactly {count} objects. Each object must have:
{{
  "uuid": "l{level}-{start:03d}" to "l{level}-{end:03d}",
  "level": {level},
  "korean_text": "<Korean sentence(s) or paragraph - the practice problem>",
  "reference_answer": "<Natural, correct English translation>",
  "topic_tag": "<one of the topic names in English, snake_case>"
}}

Requirements:
- korean_text should be natural Korean suitable for translation practice
- Level 1: exactly 1 sentence
- Level 2: exactly 2 sentences on the same topic, separated by \\n
- Level 3: exactly 3 sentences on the same topic, separated by \\n
- Level 4: a paragraph of 4-6 sentences
- reference_answer should be a natural, native-sounding English translation
- Vary difficulty within the level (some easy, some intermediate)
- Cover diverse vocabulary and grammar patterns
- Do NOT include any explanation, just the JSON array"""


def generate_batch(client: anthropic.Anthropic, level: int, start: int, end: int, batch_num: int) -> list:
    spec = LEVEL_SPECS[level]
    count = end - start + 1
    topics_str = "\n".join(f"- {t}" for t in spec["topics"])

    prompt = GENERATE_PROMPT.format(
        count=count,
        level=level,
        description=spec["description"],
        topics=topics_str,
        start=start,
        end=end
    )

    print(f"  Generating Level {level}, batch {batch_num} (items {start}-{end})...")

    for attempt in range(3):
        try:
            message = client.messages.create(
                model="claude-sonnet-4-6",
                max_tokens=8192,
                messages=[{"role": "user", "content": prompt}]
            )
            raw = message.content[0].text.strip()

            # Extract JSON array if wrapped in markdown
            if raw.startswith("```"):
                raw = raw.split("```")[1]
                if raw.startswith("json"):
                    raw = raw[4:]
                raw = raw.strip()

            items = json.loads(raw)
            if len(items) != count:
                print(f"    Warning: expected {count} items, got {len(items)}")
            return items

        except (json.JSONDecodeError, Exception) as e:
            print(f"    Attempt {attempt + 1} failed: {e}")
            if attempt < 2:
                time.sleep(2 ** attempt)

    return []


def generate_level(client: anthropic.Anthropic, level: int) -> list:
    all_problems = []
    batch_size = 20

    for batch_start in range(1, 101, batch_size):
        batch_end = min(batch_start + batch_size - 1, 100)
        batch_num = (batch_start - 1) // batch_size + 1
        batch = generate_batch(client, level, batch_start, batch_end, batch_num)
        all_problems.extend(batch)
        time.sleep(1)  # Rate limiting

    # Ensure UUIDs are correct
    for i, problem in enumerate(all_problems):
        problem["uuid"] = f"l{level}-{i + 1:03d}"
        problem["level"] = level

    return all_problems


def main():
    api_key = os.environ.get("ANTHROPIC_API_KEY")
    if not api_key:
        print("Error: ANTHROPIC_API_KEY environment variable not set")
        sys.exit(1)

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    client = anthropic.Anthropic(api_key=api_key)

    levels = [1, 2, 3, 4]
    if len(sys.argv) > 1:
        levels = [int(x) for x in sys.argv[1:]]

    for level in levels:
        output_file = OUTPUT_DIR / f"level{level}_problems.json"

        # Skip if already exists and has 100 items
        if output_file.exists():
            existing = json.loads(output_file.read_text())
            if len(existing) >= 100:
                print(f"Level {level}: already has {len(existing)} problems, skipping")
                continue

        print(f"\nGenerating Level {level} problems...")
        problems = generate_level(client, level)

        if problems:
            output_file.write_text(
                json.dumps(problems, ensure_ascii=False, indent=2),
                encoding="utf-8"
            )
            print(f"Level {level}: saved {len(problems)} problems to {output_file}")
        else:
            print(f"Level {level}: generation failed!")

    print("\nDone!")


if __name__ == "__main__":
    main()
