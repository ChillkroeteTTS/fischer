# Fischer [![Build Status](https://travis-ci.org/ChillkroeteTTS/fischer.svg?branch=master)](https://travis-ci.org/ChillkroeteTTS/fischer)
> Dedicated to Gerhard Fischer, one of the inventors of the hand held metal detector.
Also the german term for a fisherman.

Proper application monitoring and especially alarming is crucial to quickly identify failures in your production applications.
However in an environment with many different metrics and applications maintenance and setup becomes a tedious task.

Fischer aims to keep you up to date about any unusual behaviour of your metrics while keeping the configuration effort minimal.
Thus it enables you to focus on further development of your application instead of watching on your metrics the whole day.

Fischer uses an anomaly detection algorithm based on a normal distribution fitted to historical data of your metrics.
Currently only prometheus is configurable as data source, but more such as graphite will follow.
Please mind that fischer is still under heavy development, if you want to contribute feel free to tackle on of the issues.

## Usage
The entire configuration is done from a file called `config.edn` located in the working directory.
See `EXAMPLE_config.edn` for documentation.

### Lein
Do `lein run` in the repository folder and of you go.

### Docker
Just mount your `config.edn` into the home directory of `/fischer/`.

Example:

```docker run -v [PATH_TO_YOUR_CONFIG]:/fischer/config.edn fischer```

## Model Training Data
Please note that fischer can only work with metrics which have been measured for the complete time range specified in the `config.edn`.
If a metric provides less datapoints than the other metrics, the corresponding metric will be withhold from the anomaly detection algorithm.
To get detailed information about which data was used to fit the anomaly detection model, Fischer provides a JSON under
`http://[HOST]:[PORT]/models` containing detailed information about the training data.

## License

Copyright © 2017 Tjark Smalla

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
