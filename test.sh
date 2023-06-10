export TZ=UTC
mvn test  # Unit tests go here
mvn compile exec:java -Dexec.mainClass="org.example.LogCount" -Dexec.args="server_log.csv"
mvn package
java -jar target/log-metrics-1.0-SNAPSHOT-jar-with-dependencies.jar  server_log.csv



echo
echo "============================================================"
echo "==============       Functional Tests     =================="
echo "============================================================"

alias logcnt="java -jar target/log-metrics-1.0-SNAPSHOT-jar-with-dependencies.jar"

echo Test0:
    logcnt  server_log.csv
    tail -n +1 server_log.csv | grep 2020 | wc -l

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
