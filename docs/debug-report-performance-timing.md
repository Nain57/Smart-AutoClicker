# Debug Report performance timing

## Purpose

Performance timing is part of the Debug Report. Its purpose is to help a scenario author find conditions that consume
the most detection time and to show how much time the Execution Limiter deliberately kept detection idle.

This data is diagnostic. It does not directly measure battery consumption and it does not attempt to explain time
spent inside Android, other applications, or device hardware.

## Enablement and ownership

All performance timing is enabled and disabled with Debug Report generation. There is no separate condition profiler
setting or output file. The Debug Report protobuf files are the only stored source of truth.

The recorder, report models, protobuf schema, and mappings belong to the existing `core:smart:debugging` module. The
processing module contains only the timing boundary and a small synchronous listener because condition evaluation and
the Execution Limiter run there.

When Debug Report generation is disabled:

- no condition clock is read;
- no performance aggregate is updated; and
- the processing loop does not measure Execution Limiter suspension time.

## Condition measurements

A condition check starts immediately before `verifyCondition` and ends immediately after it returns. The duration
therefore covers all work performed for that condition, including bitmap retrieval and the applicable detector call.

- A check is counted only when a condition is actually reached and evaluated.
- Conditions skipped by AND/OR short-circuiting are not counted.
- Fulfilled means that the configured condition expression evaluated to true. For a negative screen condition, this
  can mean that the image, color, text, or number was not detected.
- Configured but unreached conditions remain in the report with zero checks and zero durations.

For every configured condition, the report stores its database ID, check count, fulfilled count, total duration, and
minimum and maximum duration. Durations are stored as integer nanoseconds. Nanoseconds are the storage unit and do not
claim nanosecond measurement accuracy. A report reader chooses an appropriate display unit and derives averages and
percentages from the raw values.

A condition's time share uses the sum of all condition durations as its denominator. It must be described as a share
of condition-processing time, not as a share of CPU, battery, or the whole detection session.

## Session and Execution Limiter measurements

The existing Debug Report overview stores whole-session elapsed time.

Active detection-loop time is the accumulated duration of calls to `ScenarioProcessor.process`. The processing engine
already measures each call to enforce the configured rate, so reporting the total requires only an aggregate update.
It excludes the delay inserted after a loop by the Execution Limiter.

Execution Limiter wait time measures elapsed suspension at the limiter's delay site. It is recorded only when the
user-configured limiter is enabled. Safety delays used in unlimited mode and delays caused by unavailable screen
frames or scenario actions are not limiter time. A partial limiter suspension interrupted by cancellation is retained.

Session duration minus active detection-loop time is not treated as Execution Limiter time because that difference
also contains actions and other waits.

## Lifecycle and compatibility

Aggregates are allocated and reset when a report session starts, updated synchronously from the single processing
path, snapshotted once when the session ends, and written by the existing serialized Debug Report writer.

Each session owns a fresh aggregate. Stopping, cancellation, orientation changes, or starting a later session must not
mix values between reports. Timing must never change condition results or prevent detection from stopping normally.

The condition profile is an optional protobuf message and new overview fields use new field numbers. Readers must
continue to accept older reports, for which performance fields are absent and therefore decode to zero.

## Deferred work

This foundation does not include UI, optimization recommendations, fulfilled/unfulfilled timing splits, percentiles,
histograms, CPU attribution, battery estimates, or system-process analysis. Those can be designed later without
changing the meaning of the raw measurements above.
