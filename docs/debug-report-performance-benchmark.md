# Debug Report condition-timing benchmark

## Purpose

The condition-timing prototype was benchmarked before it was converted into the permanent Debug Report architecture.
The goal was to determine whether two monotonic clock reads and one fixed-size aggregate update per reached condition
created a measurable real-world cost.

This was a device-level comparison rather than a synthetic microbenchmark. It includes the natural variance of screen
capture, image detection, game rendering, Android scheduling, and temperature. The result can therefore show whether
the added measurement stands out during real use; it cannot prove that its cost is exactly zero.

## Test setup

- Device: Xiaomi M2012K11I, Android 14, running on battery with its case removed.
- Scenario: a real game automation performing six attack/battle/exit loops before stopping itself.
- Execution Limiter: 10 loops per second for every run.
- Runs: 20 valid runs, five for each mode.
- Total measured detector runtime: 1,106.394 seconds.
- Thermal control: the four modes used a balanced rotation rather than running one mode in a single block.
- Rotation: `C A B D / A D C B / C B A D / B D A C / D B A C`.
- Observations: detector elapsed time, process CPU ticks, battery level, battery temperature, Android thermal status,
  report size, and condition-profile consistency.

The modes were:

| Mode | Existing Debug Report | Condition timing |
|---|---:|---:|
| A | Off | Off |
| B | On | Off |
| C | On | On |
| D | Off | On |

Modes A and D isolated the prototype recorder from the existing report machinery. Modes B and C measured its
incremental cost when used as intended inside the Debug Report. The independent combinations existed only for the
prototype experiment; the permanent implementation always enables condition timing with the Debug Report.

## Results

| Mode | Median elapsed | Range | Median CPU ticks | Median CPU ticks/s |
|---|---:|---:|---:|---:|
| A: neither | 54.461 s | 52.108–58.338 s | 5,049 | 92.68 |
| B: report only | 53.848 s | 52.728–64.296 s | 6,719 | 122.93 |
| C: report + timing | 53.470 s | 52.912–56.735 s | 6,703 | 125.63 |
| D: timing only | 53.105 s | 52.099–59.489 s | 4,893 | 92.22 |

The comparisons relevant to condition-timing overhead were:

- B → C, with the Debug Report already enabled: median elapsed time changed by -0.378 seconds, median raw CPU ticks
  changed by -16, and median duration-normalized CPU ticks changed by approximately +2.2%.
- A → D, with the existing report disabled: median elapsed time changed by -1.356 seconds, median raw CPU ticks
  changed by -156, and median duration-normalized CPU ticks changed by approximately -0.5%.

Negative changes are not interpreted as performance improvements. The mixed directions and their size relative to
run-to-run variance mean that the prototype's incremental cost did not stand out in this experiment. By contrast, the
existing Debug Report path produced a clearly visible increase in process CPU ticks, which indicates that the method
was capable of exposing an effect larger than the surrounding noise.

All 20 runs completed successfully. Battery temperature rose from 41 °C to 45 °C during the early rotation and then
remained at 45 °C. Android reported thermal status 0 throughout, so no run was marked as thermally throttled. Battery
level fell from 67% to 52% across the complete experiment.

## Data-integrity observations

The ten timing-enabled runs produced:

- 604,013 recorded condition checks;
- 348.080 seconds of accumulated condition-processing time;
- 29 configured conditions, of which 21 were reached;
- the expected six successful completions for each scenario milestone condition; and
- no missing or malformed profile output.

The data also demonstrated the feature's intended value. Four conditions belonging to the same event accounted for
229.524 seconds, or 65.94% of all measured condition-processing time. Two inexpensive conditions from another event
were checked more than 80,000 times each and together accounted for another 10.82%. This shows why both cumulative
time and check count are needed: an individually slow condition can be insignificant when rarely reached, while a
cheap condition can become important through repetition.

## Interpretation and limits

The supported conclusion is that the approved aggregate design introduced no **detectable** overhead under this
real-world workload. It is not a claim of mathematically zero cost, nor a general battery benchmark across devices.

The permanent implementation preserves the properties exercised by the prototype:

- no timing clock read when Debug Report generation is disabled;
- no per-check allocation, coroutine, lock, or file operation;
- primitive, fixed-size per-condition aggregates; and
- one protobuf snapshot written through the existing serialized report writer when detection ends.

Raw artifacts were retained locally during development, including per-run metadata, logs, profile CSV files, and the
scenario database used to resolve condition names. They are not committed because they include a debug APK, device
logs, and scenario-specific data; this document records the reproducible test design and aggregate results relevant to
the architecture decision.
