---
name: android-profiler
description: "Pilote le profilage et le débogage de performance Android : enregistre et analyse traces système, heap dumps, enregistrements de méthodes, échantillons de pile et allocations mémoire ; investigue goulots d'étranglement, jank, fuites mémoire et démarrage ; écrit, débogue et exécute des requêtes SQL ad hoc sur une trace. S'applique aux apps et services utilisateur ou système. Déclencher sur — « profiler l'app Android », « system trace », « heap dump », « jank », « fuite mémoire », « démarrage lent », « requête SQL sur la trace ». NE PAS utiliser pour l'optimisation R8 (r8-analyzer), les conseils de performance généraux (android-performance) ni les tests (testing-setup)."
license: Complete terms in LICENSE.txt
metadata:
  author: Google LLC
  last-updated: '2026-08-06'
  keywords:
    - Android performance
    - debugging
    - profiling
    - recording
    - trace analysis
    - memory leaks
    - bottleneck
    - jank
    - SQL
---

# Android Profiler Orchestrator

Your primary role is **Intent Disambiguation and Routing**; route the user to
the correct workflow or prepare an execution plan for the user. Work with the
user to finalize the plan and then proceed with the plan execution, addressing
singular as well as composite needs.

## Prerequisites and Setup

Before executing any workflows, read
[`references/env_setup.md`](references/env_setup.md) (it sits next to this file
in the skill root). It defines what to set `$SKILL_ROOT` to - the anchor every
other path in this skill is written against.

## Intent Disambiguation

Do not guess the user's intent. If the user request is not clear, **ask the
user** what they want to do before proceeding.

## Recording

Route all recording requests through
`$SKILL_ROOT/recording/recording_orchestrator.md`. This defines guidelines and
pre-flight checks or dependency checks that apply to all recording workflows,
and ensures you have the necessary setup to proceed. Read the orchestrator and
execute the plan it describes based on what the user wants to record (for
example, a system trace or a heap dump).

## Analysis

Route all analysis requests through
`$SKILL_ROOT/analysis/analysis_orchestrator.md`.