# Conan

Proper application monitoring and especially alarming is crucial to quickly identify failures in your production applications.
However in an environment with many different metrics and applications maintenance and setup becomes a tedious task.

Conan aims to keep you up to date about any unusual behaviour of your metrics while keeping the configuration effort minimal.
Thus it enables you to focus on further development of your application instead of watching on your metrics the whole day.

Conan uses an anomaly detection algorithm based on a normal distribution fitted to historical data of your metrics.
Currently only prometheus is configurable as data source, but more such as graphite will follow.
Please mind that conan is still under heavy development, if you want to contribute feel free to tackle on of the tasks.
The main topics to come are:
- Use an efficient math library for the anomaly detection algorithm (such as neanderthal)
- Implement a reporter system to send alarms (http-call, mail etc.)
- Implement a graphite provider
- Figure out a good value for epsylon (almost no false positives, maybe different alarm modes?)
- Train model in a configurable interval

## Usage
The entire configuration is done from a file called `config.edn` located in the working directory.
See `EXAMPLE_config.edn` for documentation.

## License

Copyright © 2017 Tjark Smalla

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
