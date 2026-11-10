#!/usr/bin/env python3
"""Cross-check finite native coverage. Does not infer arbitrary modpack/renderer compatibility."""
import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SINCE = None

def load(name):
    path = ROOT / 'run' / name
    assert path.is_file(), f'Missing evidence: {path}'
    assert SINCE is None or path.stat().st_mtime >= SINCE, f'Stale evidence cannot satisfy this run: {path}'
    return json.loads(path.read_text())

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path)
    parser.add_argument('--since', type=float, help='Reject JSON evidence older than this Unix epoch')
    args = parser.parse_args()
    global SINCE
    SINCE = args.since
    tp = load('bbs-tacz-motion-catalog.json')
    fp = load('bbs-tacz-first-person-catalog.json')
    gameplay = load('bbs-tacz-default-gameplay.json')
    environment = load('bbs-tacz-live-environment.json')
    editor = load('bbs-tacz-editor-audio-modes.json')
    assert tp and fp and gameplay
    tp_guns, fp_guns, actual_guns = ({r['gun'] for r in rows} for rows in (tp, fp, gameplay))
    assert tp_guns == fp_guns == actual_guns, 'TP/FP/native input gun inventories differ (a filtered probe is not full coverage)'
    assert len(actual_guns) >= 70, 'Expected the complete pinned default pack, not representative guns only'
    for rows in (tp, fp, gameplay, environment, editor):
        assert all(r.get('status', 'pass').lower() in ('pass', 'not_applicable') for r in rows), 'Evidence contains a failed case'
    assert len({(r['gun'], r['mode']) for r in gameplay}) == len(gameplay) >= 110
    for row in gameplay:
        assert row['distance'] > 0.2 and row['client_uses'] > 0 and row['server_uses'] > 0 and row['native_sound_cues'] > 0
        assert row['charge'] in ('PASS', 'N/A') and row['heat'] in ('PASS', 'N/A')
    assert sum(r['charge'] == 'PASS' for r in gameplay) >= 3, 'The declared stock charges were not exercised'
    assert any(r['heat'] == 'PASS' for r in gameplay), 'Native stock overheating was not reached'
    assert {r['motion'] for r in environment} == {'backward_walk','strafe_turn','ladder_climb','swim','boat_ride','elytra_glide','creative_flight'}
    assert all(r['distance'] > 1 and r['native_shots'] > 0 and r['reload_events'] > 0 and r['melee_events'] > 0 for r in environment)
    workflows = {}
    expected = {'stand','walk','sprint_to_aim','crouch_walk','crawl_move','jump'}
    for name in ('MovingGunWorkflowReview','MovingF4WorkflowReview','MovingThirdPersonWorkflowReview'):
        rows = load('bbs-tacz-live-motion-' + name + '.json')
        assert {r['motion'] for r in rows} == expected
        assert all(r['status'].lower() == 'pass' and r['reload_events'] > 0 and r['melee_events'] > 0 for r in rows)
        assert all(r['distance'] > 1 for r in rows if r['motion'] != 'stand')
        assert next(r for r in rows if r['motion'] == 'jump')['max_height'] > 0.4
        workflows[name] = rows
    report = {
        'status': 'PASS',
        'scope': 'Pinned stock pack, integrated server, native renderer; controlled catalogs are not legal-input Cartesian products',
        'guns': len(actual_guns), 'third_person_controlled_rows': len(tp), 'first_person_controlled_rows': len(fp),
        'native_gun_mode_pairs': len(gameplay), 'native_charge_cases': sum(r['charge'] == 'PASS' for r in gameplay),
        'native_heat_cases': sum(r['heat'] == 'PASS' for r in gameplay),
        'environment': environment, 'editor_checks': editor, 'dynamic_workflows': workflows,
        'not_claimed': ['Every third-party gun pack', 'Every shader/Sodium combination', 'Independent multiplayer servers', 'Every ordering of every input in every environment']
    }
    text = json.dumps(report, ensure_ascii=False, indent=2) + '\n'
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(text)
    print(text, end='')

if __name__ == '__main__':
    main()
