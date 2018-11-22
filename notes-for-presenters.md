# Notes for Presenters

Dean Wampler, November 2018

Here are some suggestions for presenters of this tutorial. It's designed to have too much material to cover in depth in a 1/2-day session, in which there is no real time for exercises, but plenty of "homework" options for students. For a full-day session, there is enough material to cover in some depth, leaving about an hour for exercises. Or, if the students don't seem that interested in trying the exercises, you could walk through the code in even more detail...

For a 1/2-day session, you'll need to skip some slides or go through them very quickly. Use your judgment. You might tell the students that "these slides cover an important topic, but we won't have time to cover them here, so look at them later."

Similarly, for a 1/2-day session, you can't walk through all the code. Show the top-level projects, where the main pieces are organized, then briefly walk through the core parts of the model serving logic in both sample apps. For a full-day class, you can spend more time walking through almost all the code.

## Tutorial Schedule

This is the proposed time line when doing a 1/2-day or a full-day tutorial. Times shown are in minutes. Most conferences plan breaks every 1.5 hours, which are also shown in this schedule, since you'll want to stop at a reasonable point.

## 1/2 Day: ~3 Hours

A three-hour tutorial, usually covering 3.5 hours with a 30-minute break. Typical morning and afternoon schedules are shown for easier tracking. Note that some parts are very short!

| Time      | Morning       | Afternoon     | Topics |
| --------: | ------------: | ------------: | :----- |
|  00 -  10 |  9:00 -  9:10 | 13:30 - 13:40 | Introductions  |
|  10 -  30 |  9:10 -  9:30 | 13:40 - 14:00 | Why Kafka (resist the temptation to dwell on Kafka best practices, etc.) |
|  30 -  35 |  9:30 -  9:35 | 14:00 - 14:05 | Overview of the four streaming engines  |
|  35 -  55 |  9:35 -  9:55 | 14:05 - 14:25 | Challenges of model serving in a streaming context  |
|  55 -  65 |  9:55 - 10:05 | 14:25 - 14:35 | Spectrum of data in microservices, motivation for Akka Streams vs. Kafka Streams  |
|  65 -  75 | 10:05 - 10:15 | 14:35 - 14:45 | Akka Streams: description  |
|  75 -  90 | 10:15 - 10:30 | 14:45 - 15:00 | Akka Streams: example walkthrough  |
|  90 - 120 | 10:30 - 11:00 | 15:00 - 15:30 | Break  |
| 120 - 130 | 11:00 - 11:10 | 15:30 - 15:40 | Akka Streams: other production concerns  |
| 130 - 145 | 11:10 - 11:25 | 15:40 - 15:55 | Kafka Streams: description  |
| 145 - 180 | 11:25 - 12:00 | 15:55 - 16:30 | Kafka Streams: example walkthrough  |
| 180 - 190 | 12:00 - 12:10 | 16:30 - 16:40 | Model Serving: other production concerns  |
| 190 - 210 | 12:10 - 12:30 | 16:40 - 17:00 | Wrap up and final questions  |


## Full Day: ~6 Hours

A six-hour tutorial, usually covering eight hours with two, 30-minute breaks and a one-hour lunch. The same sections in the 1/2-day session are here, but more time allows more depth of discussion. There is one additional section, for exercises (plus lunch and a second break...).

| Time      | Clock         | Topics |
| --------: | ------------: | :----- |
|  00 -  10 |  9:00 -  9:10 | Introductions  |
|  10 -  40 |  9:10 -  9:40 | Why Kafka  |
|  40 -  50 |  9:40 -  9:50 | Overview of four streaming engines  |
|  50 -  80 |  9:50 - 10:20 | Challenges of model serving in a streaming context  |
|  80 -  90 | 10:20 - 10:30 | Spectrum of data in microservices, motivation for Akka Streams vs. Kafka Streams  |
|  90 - 120 | 10:30 - 11:00 | Break  |
| 120 - 135 | 11:00 - 11:15 | Akka Streams: description  |
| 135 - 180 | 11:15 - 12:00 | Akka Streams: example walkthrough, in depth  |
| 180 - 210 | 12:00 - 12:30 | Akka Streams: other production concerns  |
| 210 - 270 | 12:30 - 13:30 | Lunch  |
| 270 - 290 | 13:30 - 13:50 | Kafka Streams: description  |
| 290 - 360 | 13:50 - 15:00 | Kafka Streams: example walkthrough, in depth  |
| 360 - 390 | 15:00 - 15:30 | Break  |
| 390 - 400 | 15:30 - 15:40 | Model Serving: other production concerns  |
| 400 - 460 | 15:40 - 16:40 | Exercises  |
| 460 - 480 | 16:40 - 17:00 | Wrap up and final questions  |
