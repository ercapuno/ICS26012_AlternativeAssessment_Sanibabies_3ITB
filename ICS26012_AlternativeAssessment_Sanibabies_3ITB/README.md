# CPU and Disk Scheduling Simulator

Java Swing desktop app for the Operating Systems project. Uses only the Java standard library and runs on Java 8 or newer.

Working modules: **Priority (Non-Preemptive)**, **Multilevel Queue** (queue = Priority field; 2 to 5 queues, each with its own algorithm: Round Robin, FCFS, SJF or SRTF; default Q1 RR q=4, Q2 RR q=8, Q3 FCFS; higher queue preempts), **Shortest Seek Time First (SSTF)** and **C-LOOK**. All four open from the main menu.

## Compile and run

Run these from the project folder (the one that contains `src`). The commands work in Command Prompt and PowerShell.

```
javac -d out src\*.java src\model\*.java src\scheduler\*.java src\gui\*.java src\prioritynp\*.java src\multilevelqueue\*.java src\sstf\*.java src\clook\*.java
java -cp out Main
```

On macOS/Linux, use forward slashes: `javac -d out src/*.java src/model/*.java src/scheduler/*.java src/gui/*.java src/prioritynp/*.java src/multilevelqueue/*.java src/sstf/*.java src/clook/*.java`.

### Run the algorithm check (no GUI)

```
javac -cp out -d out-test test\PriorityNonPreemptiveTest.java
java -cp "out;out-test" PriorityNonPreemptiveTest
```

On macOS/Linux, use `out:out-test` instead of `out;out-test`. The check prints PASS or FAIL for each item and ends with "All checks passed."

## Project layout

```
src/
  Main.java                          entry point
  MainMenu.java                      menu; About dialog text (group name, members) is at the top

  prioritynp/                        PRIORITY (NON-PREEMPTIVE) MODULE - owner: Erwin G. Capuno
    PriorityNonPreemptive.java       the algorithm (no GUI code)
    PriorityNPPanel.java             input/output window and the Sample window
    PrioritySampleData.java          the two sample problems

  multilevelqueue/                   MULTILEVEL QUEUE MODULE - owner: Ranz Cyrus Cutarra
    MultilevelQueue.java, MLQPanel.java, MLQSampleData.java

  sstf/                              SSTF MODULE - owner: Wynn Benedict De Villa
    SSTF.java                        the algorithm (no GUI code)
    SstfPanel.java                   input/output window and the Sample window
    SstfSampleData.java              the two sample problems

  clook/                             C-LOOK MODULE - owner: Jayvi Al Andres
    CLook.java, CLookPanel.java, CLookSampleData.java

  Shared by all modules:
  model/Process.java                 PID, arrival, burst, priority + recorded times
  model/GanttBlock.java              one Gantt bar (label, start, end, idle?)
  model/ScheduleResult.java          Gantt blocks, processes, metrics (shared by all CPU modules)
  model/DiskResult.java              seek sequence + total head movement (for disk modules)
  scheduler/CpuScheduler.java        interface for CPU algorithms
  scheduler/DiskScheduler.java       interface for disk algorithms
  gui/ModuleLauncher.java            how the menu opens a module
  gui/GanttChartPanel.java           custom-painted Gantt chart
  gui/SeekPathPanel.java             head-path graph (SSTF)
  gui/SeekChartPanel.java            head-path graph (C-LOOK)
test/
  PriorityNonPreemptiveTest.java     checks the algorithm against the hand-worked samples below
  MultilevelQueueTest.java           checks Multilevel Queue
  SstfTest.java                      checks SSTF (samples, ties, duplicates, boundaries)
  CLookTest.java                     checks C-LOOK (both directions, jump, duplicates, boundaries)
```


## Sample data and expected output

Lower priority number = higher priority. Ties: earlier arrival, then lower PID.

| PID | Arrival | Burst | Priority |
|-----|--------:|------:|---------:|
| P1  | 0       | 3     | 3        |
| P2  | 1       | 4     | 1        |
| P3  | 2       | 2     | 1        |
| P4  | 3       | 1     | 2        |
| P5  | 13      | 3     | 2        |
| P6  | 13      | 2     | 2        |
| P7  | 14      | 2     | 0        |
| P8  | 15      | 1     | 4        |
| P9  | 20      | 2     | 3        |
| P10 | 16      | 3     | 2        |

### Worked by hand

| Time | Arrived and waiting | Decision |
|-----:|---------------------|----------|
| 0  | P1 (pr 3) | Only P1 has arrived, so **P1 runs 0–3**. P2 (priority 1) arrives at 1 but cannot preempt. |
| 3  | P2 (pr 1, arr 1), P3 (pr 1, arr 2), P4 (pr 2) | **Priority tie:** P2 and P3 both have priority 1. P2 arrived earlier, so **P2 runs 3–7**. |
| 7  | P3 (pr 1), P4 (pr 2) | **P3 runs 7–9**. |
| 9  | P4 (pr 2) | **P4 runs 9–10**. |
| 10 | none (next arrivals at 13) | **IDLE 10–13**. |
| 13 | P5 (pr 2, arr 13), P6 (pr 2, arr 13) | **Tie on priority and arrival:** lower PID wins, so **P5 runs 13–16**. P7 (priority 0) arrives at 14 but cannot preempt. |
| 16 | P6 (pr 2, arr 13), P7 (pr 0), P8 (pr 4), P10 (pr 2, arr 16) | P7 has the highest priority, so **P7 runs 16–18**. |
| 18 | P6 (pr 2, arr 13), P8 (pr 4), P10 (pr 2, arr 16) | **Priority tie:** P6 and P10 both have priority 2. P6 arrived earlier, so **P6 runs 18–20**. |
| 20 | P8 (pr 4), P9 (pr 3, arr 20), P10 (pr 2) | **P10 runs 20–23**. |
| 23 | P8 (pr 4), P9 (pr 3) | **P9 runs 23–25**. |
| 25 | P8 (pr 4) | **P8 runs 25–26**. It arrived at 15 but waited the longest because it has the lowest priority and there is no aging. |

**Gantt chart**

```
| P1 | P2 | P3 | P4 | IDLE | P5 | P7 | P6 | P10 | P9 | P8 |
0    3    7    9    10     13   16   18   20    23   25   26
```

**Per-process results** (TAT = CT − AT, WT = TAT − BT, RT = first CPU time − AT)

| PID | AT | BT | Pr | CT | TAT | WT | RT |
|-----|---:|---:|---:|---:|----:|---:|---:|
| P1  | 0  | 3  | 3  | 3  | 3   | 0  | 0  |
| P2  | 1  | 4  | 1  | 7  | 6   | 2  | 2  |
| P3  | 2  | 2  | 1  | 9  | 7   | 5  | 5  |
| P4  | 3  | 1  | 2  | 10 | 7   | 6  | 6  |
| P5  | 13 | 3  | 2  | 16 | 3   | 0  | 0  |
| P6  | 13 | 2  | 2  | 20 | 7   | 5  | 5  |
| P7  | 14 | 2  | 0  | 18 | 4   | 2  | 2  |
| P8  | 15 | 1  | 4  | 26 | 11  | 10 | 10 |
| P9  | 20 | 2  | 3  | 25 | 5   | 3  | 3  |
| P10 | 16 | 3  | 2  | 23 | 7   | 4  | 4  |
| **Sum** | | | | | **60** | **37** | **37** |

**Summary**

- Average Turnaround Time = 60 / 10 = **6.00 ms**
- Average Waiting Time = 37 / 10 = **3.70 ms**
- Average Response Time = 37 / 10 = **3.70 ms** (same as waiting, as expected for non-preemptive)
- Total time = last completion = 26; busy time = sum of bursts = 23 (idle = 3)
- CPU Utilization = 23 / 26 × 100 = **88.46%**
- Throughput = 10 / 26 = **0.38 processes/ms**

## Sample 2 (shown only in the Sample window)

Textbook problem: all processes arrive at time 0, in the order P1–P5; burst times are in milliseconds.

| PID | Arrival | Burst | Priority |
|-----|--------:|------:|---------:|
| P1  | 0       | 10    | 3        |
| P2  | 0       | 1     | 1        |
| P3  | 0       | 2     | 4        |
| P4  | 0       | 1     | 5        |
| P5  | 0       | 5     | 2        |

All processes are ready at 0 and they never interrupt one another, so they simply run in priority order: P2 (1), P5 (2), P1 (3), P3 (4), P4 (5).

```
| P2 | P5 | P1 | P3 | P4 |
0    1    6    16   18   19
```

| PID | CT | TAT | WT | RT |
|-----|---:|----:|---:|---:|
| P1  | 16 | 16  | 6  | 6  |
| P2  | 1  | 1   | 0  | 0  |
| P3  | 18 | 18  | 16 | 16 |
| P4  | 19 | 19  | 18 | 18 |
| P5  | 6  | 6   | 1  | 1  |
| **Sum** | | **60** | **41** | **41** |

- Average Turnaround Time = 60 / 5 = **12.00 ms**
- Average Waiting Time = 41 / 5 = **8.20 ms**
- Average Response Time = 41 / 5 = **8.20 ms**
- CPU Utilization = 19 / 19 × 100 = **100.00%** (no idle time)
- Throughput = 5 / 19 = **0.26 processes/ms**

## Multilevel Queue: configurable queues

In the **Queue setup** box of the Multilevel Queue window you choose:

- **Number of queues**: 2 to 5. Queue 1 has the highest priority.
- **Algorithm of each queue**: Round Robin, FCFS, SJF or SRTF.
- **Quantum**: only for Round Robin (whole number 1 to 9999). The field is greyed out for the other algorithms.

The **Queue** column of the process table is the queue number (1 to the number of queues chosen). Changing the setup clears the old results; **Load Sample Data** and **Reset/Clear** go back to the default setup (3 queues: RR q=4, RR q=8, FCFS).

How each algorithm behaves inside its queue:

| Algorithm | Behaviour |
|-----------|-----------|
| Round Robin | Runs for at most the quantum, then goes to the back of its queue. |
| FCFS | First come, first served; runs until it finishes. |
| SJF | Non-preemptive: the shortest burst runs next and is never interrupted by a process of the same queue. |
| SRTF | Preemptive: a process arriving in the same queue with a strictly shorter remaining time takes the CPU. |

Rules shared by all queues: a lower queue runs only when every higher queue is empty, and a process arriving in a higher queue preempts the running process immediately. A Round Robin / FCFS process preempted this way goes back to the front of its queue; an SJF process that was cut off resumes before other jobs of its queue; SRTF compares remaining times again. Ties in SJF and SRTF go to the earlier arrival, then the earlier input row.

## Multilevel Queue samples

Both Multilevel Queue samples use the default 3-queue setup and the same processes (PID, arrival, burst) as the Priority samples above. The Priority field is the queue number, so the priorities 0 to 5 are mapped to the three queues: priority 0-1 -> Queue 1 (RR q=4), 2-3 -> Queue 2 (RR q=8), 4-5 -> Queue 3 (FCFS).

**Sample 1** (10 processes, idle gap): P2, P3, P7 are in Q1; P1, P4, P5, P6, P9, P10 in Q2; P8 in Q3. P2 preempts P1 at 1, and P7 preempts P5 at 14.

```
| P1 | P2 | P3 | P1 | P4 | IDLE | P5 | P7 | P5 | P6 | P10 | P9 | P8 |
0    1    5    7    9    10     13   14   16   18   20    23   25   26
```

Average Turnaround = 62 / 10 = **6.20**, Waiting = 39 / 10 = **3.90**, Response = 31 / 10 = **3.10**, CPU Utilization = 23 / 26 = **88.46%**.

**Sample 2** (5 processes, all arrive at 0): P2 in Q1; P1, P5 in Q2; P3, P4 in Q3.

```
| P2 | P1 | P5 | P1 | P3 | P4 |
0    1    9    14   16   18   19
```

Average Turnaround = 68 / 5 = **13.60**, Waiting = 49 / 5 = **9.80**, Response = 44 / 5 = **8.80**, CPU Utilization = **100.00%**.

## Input rules

- Number of processes: 10 to 20.
- PID: required, unique (case-insensitive, so `p1` and `P1` count as the same).
- Arrival Time and Priority: whole numbers ≥ 0.
- Burst Time: whole number ≥ 1.
- All numbers must be at most 9999, which keeps the chart readable and prevents overflow.
- Empty cells, text, decimals and negative numbers are rejected. The error message names the row and column, and that cell is highlighted.

## SSTF samples

Conventions: cylinders 0 to N-1; head movement = sum of |next - current|; ties go to the lower cylinder; duplicate requests are served back-to-back (zero movement). Average seek distance = total / number of requests. Seek time = total x seek rate (seek rate is optional).

**Sample 1** (20 requests, head 100, disk 200, seek rate 4): `110 90 45 160 12 199 45 130 75 0 182 150 60 25 118 100 140 5 170 35`

```
100 -> 100 -> 90 -> 75 -> 60 -> 45 -> 45 -> 35 -> 25 -> 12 -> 5 -> 0 -> 110 -> 118 -> 130 -> 140 -> 150 -> 160 -> 170 -> 182 -> 199
```

Total head movement = **299**, average seek distance = 299 / 20 = **14.95**, seek time = 299 x 4 = **1196 ms**. Shows a request at the head (0 movement), a tie (90 vs 110, both 10 away, 90 wins), a duplicate (45, 45) and both disk ends.

**Sample 2** (textbook: head 53, disk 200, seek rate 4): `98 183 37 122 14 124 65 67`

```
53 -> 65 -> 67 -> 37 -> 14 -> 98 -> 122 -> 124 -> 183
```

Total = **236**, average = 236 / 8 = **29.50**, seek time = **944 ms**.

## C-LOOK samples

Conventions: the head moves in the chosen direction (Up or Down) to the last pending request, then jumps to the farthest request on the other end and continues in the same direction. The return jump is counted in the total head movement and drawn as a dashed line. Requests on the head's own cylinder are served first; duplicates are served back-to-back.

Both C-LOOK samples use exactly the same data as the SSTF samples, so the two disk algorithms can be compared directly.

**Sample 1** (same 20 requests as SSTF Sample 1: head 100, disk 200, **Down**, seek rate 4)

```
100 -> 100 -> 90 -> 75 -> 60 -> 45 -> 45 -> 35 -> 25 -> 12 -> 5 -> 0 -> 199 -> 182 -> 170 -> 160 -> 150 -> 140 -> 130 -> 118 -> 110
```

Total = 100 + jump 199 + 89 = **388**, average = 388 / 20 = **19.40**, seek time = **1552 ms**.

**Sample 2** (same textbook problem as SSTF Sample 2: head 53, disk 200, **Up**, seek rate 4)

```
53 -> 65 -> 67 -> 98 -> 122 -> 124 -> 183 -> 14 -> 37
```

Total = 130 + jump 169 + 23 = **322**, average = 322 / 8 = **40.25**, seek time = **1288 ms**.

### SSTF vs C-LOOK on the same data

| Data set | SSTF total / average / seek time | C-LOOK total / average / seek time |
|---|---|---|
| Sample 1 (20 requests, head 100; C-LOOK Down) | 299 / 14.95 / 1196 ms | 388 / 19.40 / 1552 ms |
| Sample 2 (textbook, head 53; C-LOOK Up) | 236 / 29.50 / 944 ms | 322 / 40.25 / 1288 ms |

## Disk input rules (SSTF and C-LOOK)

- Disk size (track size): whole number 1 to 10000; cylinders run from 0 to disk size - 1.
- Initial head (current position) and every request: whole number inside the disk.
- Seek rate (ms per cylinder): optional; whole number or decimal from 0 to 1000. Seek time = total head movement x seek rate.
- SSTF: 1 to 100 requests, typed in one field separated by commas or spaces.
- C-LOOK: 1 to 50 requests in the table, plus a direction (Up or Down).

## Run all the algorithm checks (no GUI)

```
javac -cp out -d out-test test\*.java
java -cp "out;out-test" PriorityNonPreemptiveTest
java -cp "out;out-test" MultilevelQueueTest
java -cp "out;out-test" SstfTest
java -cp "out;out-test" CLookTest
```

On macOS/Linux, use `test/*.java` and `out:out-test`.

DONED