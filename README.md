# Quick Run
sh test.sh

# Understanding of assignment

1. Command line program.  Not a service.
2. Log is the source of truth.  Program reads entire log when executed.
3. Parameters are used as AND filters. Eg user==jeff22 AND file size > 50kB
4. The first line of the log file contains the column titles
5. User's time zone is also UTC  To compare apples to apples , set TZ=UTC

# Compile and Run detaols
export TZ=UTC
mvn test
mvn compile exec:java -Dexec.mainClass="org.example.LogCount" -Dexec.args="server_log.csv"
mvn package
java -jar target/log-metrics-1.0-SNAPSHOT-jar-with-dependencies.jar  server_log.csv
alias logcnt="java -jar target/log-metrics-1.0-SNAPSHOT-jar-with-dependencies.jar"
logcnt  server_log.csv


# Usage
log-count count-item [filter] ...
    filter:
        --gt size    (Not implemented: --eg size, --lt size)
        --op operation (upload/download)
        --user username
        --on date    (Not implemented: --before date, --after date)

​
​​
# Unit Tests
- Not in scope: Parser tests the ArgFilter class would be extensively unit tested to ensure that the propper parameter values and dates are captured. In particular, all common negative tests and boundary tests should be included.
User: --user=jeff22, --user=NotExist, --user="Name with Space"
Size: 

​​
# Functional Tests
To build the code and run the tests, simply run:
sh test.sh

It will execute the following test cases.
echo Test 0:
    logcnt  server_log.csv 
    wc -l server_log.csv

echo Test1:
    logcnt   --op=download server_log.csv 
    grep "download" server_log.csv | wc -l

    logcnt   --op=upload server_log.csv 
    grep "upload" server_log.csv | wc -l

echo Test2:
    logcnt   --op=download -on="2020-04-13" server_log.csv 
    grep "Apr 13" server_log.csv | grep download | wc -l

    logcnt  --op=upload -on="2020-04-13" server_log.csv 
    grep "Apr 13" server_log.csv | grep upload | wc -l

echo Test3:
    logcnt   --user=jeff22 --op=upload server_log.csv
    grep "jeff22" server_log.csv | grep upload | wc -l

    logcnt   --user=jeff22 --op=download server_log.csv
    grep "jeff22" server_log.csv | grep download | wc -l

echo Test4:
    logcnt --user=jeff22 --on="2020-04-13" --op=download server_log.csv
    grep "jeff22" server_log.csv | grep download |grep "Apr 13" | wc -l

    logcnt  --user=jeff22 --op=upload -on="2020-04-13" server_log.csv 
    grep "jeff22" server_log.csv | grep upload | grep "Apr 13" | wc -l