# Run a java spark application on Kubernetes and HDFS

## Setup Standalone HDFS

### Update Packagelist

```bash
sudo apt-get update
```

### Install Open JDK

```bash
sudo apt-get install default-jdk
```

### Setup passwordless SSH

```bash
sudo apt-get install openssh-server openssh-client 
```

Generate Public and Private Key Pairs with the following command

```bash
ssh-keygen -t rsa
cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys
```

Verify the password-less ssh configuration

```bash
ssh localhost
```

### Install Hadoop
Go to http://hadoop.apache.org/releases.html and get the download link for the latest Hadoop distro
Eg: http://mirrors.gigenet.com/apache/hadoop/common/hadoop-3.1.2/hadoop-3.1.2.tar.gz


```bash
wget http://mirrors.gigenet.com/apache/hadoop/common/hadoop-3.1.2/hadoop-3.1.2.tar.gz
```

### Extract tar

```bash
tar -xzvf hadoop-3.1.2.tar.gz
```
Move Hadoop

```bash
sudo mv hadoop-3.1.2 /usr/local/hadoop
```


### Configure Hadoop JAVA HOME

Find default Path

```bash
readlink -f /usr/bin/java | sed "s:bin/java::"
```


### Configure Hadoop JAVA HOME

Find Java default Path

```bash
readlink -f /usr/bin/java | sed "s:bin/java::"
```

Edit hadoop-env.sh to include the following line

```bash
sudo nano /usr/local/hadoop/etc/hadoop/hadoop-env.sh
```
Include 

export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/jre/

### Setup Environment variables


```bash
export HADOOP_HOME=/usr/local/hadoop
export HADOOP_INSTALL=$HADOOP_HOME
export HADOOP_MAPRED_HOME=$HADOOP_HOME
export HADOOP_COMMON_HOME=$HADOOP_HOME
export HADOOP_HDFS_HOME=$HADOOP_HOME
export YARN_HOME=$HADOOP_HOME
export HADOOP_COMMON_LIB_NATIVE_DIR=$HADOOP_HOME/lib/native
export PATH=$PATH:$HADOOP_HOME/sbin:$HADOOP_HOME/bin
export HADOOP_OPTS="-Djava.library.path=$HADOOP_HOME/lib/native"
export HADOOP_CONF_DIR=${HADOOP_CONF_DIR:-"/usr/local/hadoop/etc/hadoop"}
```

Source the .bashrc in current login session


```bash
source ~/.bashrc
```


### Update Configurations
All files will be found under /usr/local/hadoop/etc/hadoop

Update core-site.xml

```xml
<configuration>
	<property>
	<name>fs.defaultFS</name>
	<value>hdfs://192.168.100.113:9000</value>
	</property>
	<property>
	<name>hadoop.tmp.dir</name>
	<value>/usr/local/hadoop/hadooptmpdata</value>
	</property>

</configuration>

```

Create a hadooptmpdata directory under /usr/local/hadoop


```bash
mkdir hadooptmpdata
```
 

Update hdfs-site.xml

```xml
<configuration>
        <property>
			<name>dfs.replication</name>
			<value>1</value>
			<name>dfs.name.dir</name>
			<value>file:///home/ubuntu/hdfs/namenode</value>
			<name>dfs.data.dir</name>
			<value>file:///home/ubuntu/hdfs/datanode</value>
		</property>
</configuration>
```


Create a hdfs/namenode and hdfs/datanode directory under the user home (Eg. /home/ubuntu)


```bash
mkdir -p hdfs/namenode
mkdir -p hdfs/datanode
```



Update mapred-site.xml


```xml
<configuration>
        <property>
			<name>mapreduce.framework.name</name>
			<value>yarn</value>
		</property>
</configuration>

```

Update yarn-site.xml


```xml
<configuration>

<!-- Site specific YARN configuration properties -->
	<property>
		<name>mapreduceyarn.nodemanager.aux-services</name>
		<value>mapreduce_shuffle</value>
	</property>
</configuration>


```

### Start Cluster

Format the namenode before using it for the first time. As HDFS user run the below command to format the Namenode.

```bash
hdfs namenode -format
```


Start HDFS

Navigate to /usr/local/hadoop/sbin

```bash
./start-dfs.sh
```

Start Yarn

```bash
./start-yarn.sh
```

Check Daemon status

```bash
jps
```

### Deploy Spark Operator

Download and install the [Helm CLI](https://github.com/helm/helm/releases) if you haven't already done so.

Create a service account for Tiller and bind it to the cluster-admin role. 


```bash
kubectl create serviceaccount --namespace kube-system tiller

kubectl create clusterrolebinding tiller-clusterrolebinding --clusterrole=cluster-admin --serviceaccount=kube-system:tiller
```

Deploy Helm using the service account by running the following command:

```bash
helm init --service-account tiller
```

Download the latest helm charts from GIT

```bash
git clone https://github.com/helm/charts.git
```


Create a namespace called monitoring

```bash
kubectl create ns spark-operator
kubectl create ns spark-jobs
```

Deploy the spark operator

Navigate to /charts

The Helm chart has a configuration option called sparkJobNamespace which defaults to default. For example, If you would like to run your Spark job in another namespace called spark-operator, then first make sure it already exists and then install the chart with the command

```bash
helm install --name=sparkv1 incubator/sparkoperator --namespace spark-operator --set sparkJobNamespace=spark-jobs --set serviceAccounts.spark.name=spark
```

### Word Counter Project

The project SparkTestApp contains a java spark application called MyWordCounter

The input text is read from a hdfs volume , hardcoded here to hdfs://192.168.100.113:9000/usr/local/hadoop/input and the output is written to a hdfs volume hdfs://192.168.100.113:9000/usr/local/hadoop/testk82

Note: this can be just environment variables


Create a jar file of the project. Build the project using gradle clean build command , this will create a SparkTestApp-1.0.jar

The pks.txt file contains a sample file which will be processed by the Word Counter program


### Creating the environment to run spark

Create a hdfs input and output directory and a jar directory to store the SparkTestApp-1.0.jar
```bash
hdfs dfs -mkdir -p /usr/local/hadoop/input
hdfs dfs -mkdir -p /usr/local/hadoop/output
hdfs dfs -mkdir -p /usr/local/hadoop/jars
hdfs dfs -chown -R root /usr/local/hadoop
```

Clone the sparkwordcountonk8 git repository

```bash
git clone https://github.com/riazvm/sparkwordcountonk8.git
```

Copy SparkTestApp/pks.txt to hdfs input dir

```bash
 hdfs dfs -put pks.txt /usr/local/hadoop/input
```

Copy SparkTestApp/build/libs/SparkTestApp-1.0.jar to hdfs jar dir

This is from where we will be running our spark job in K8

```bash
 hdfs dfs -put SparkTestApp-1.0.jar /usr/local/hadoop/jars
```

### Run Word count sample in PKS using spark operator

Define the Spark Application deployment

Copy the below contents to a file spark-word.yml

This yaml defines a Spark application . 


metadata:name - name of applciation
metadata:namespace - namespace that the spark job will run in. This will be the same as the one defined when we created the spark operator
spec:type - The Spark application is written in Java 
spec:mainClass - Mainclass along with Packagename
mainApplicationFile - here we are taking it from hdfs . We had copied the SparkTestApp-1.0.jar to the hdfs jar folder
Note: This can be a local directory that is accessible to all nodes in the cluster Eg: NFS
Can be S3 or http as well


```yml
apiVersion: "sparkoperator.k8s.io/v1beta1"
kind: SparkApplication
metadata:
  name: spark-word
  namespace: spark-jobs
spec:
  type: Java
  mode: cluster
  image: "gcr.io/spark-operator/spark:v2.4.0"
  imagePullPolicy: Always
  mainClass: com.test.sprak.k8.sample.MyWordCounter
  mainApplicationFile: "hdfs://192.168.100.113:9000/usr/local/hadoop/jars/SparkTestApp-1.0.jar"
  sparkVersion: "2.4.0"
  restartPolicy:
    type: Never
  volumes:
    - name: "test-volume"
      hostPath:
        path: "/tmp"
        type: Directory
  driver:
    cores: 0.1
    coreLimit: "200m"
    memory: "512m"
    labels:
      version: 2.4.0
    serviceAccount: spark
    volumeMounts:
      - name: "test-volume"
        mountPath: "/tmp"
  executor:
  	cores: 1
    instances: 1
    memory: "512m"
    labels:
      version: 2.4.0
    volumeMounts:
      - name: "test-volume"
        mountPath: "/tmp"

```

Run the spark-word applciation
Running the  command will create a SparkApplication object named spark-word

```bash
 kubectl apply -f spark-word.yml
```

Set context to spark-jobs namespace

```bash
 kubectl config set-context my-cluster --namespace spark-jobs
```
Check the object by running the following command:

```bash
kubectl get sparkapplications spark-word -o=yaml
```

To check events for the SparkApplication object, run the following command

```bash
kubectl describe sparkapplication spark-word
```

To check pods running

```bash
kubectl get po --all-namespaces
```


To check logs

```bash
kubectl logs <podname>
```


Check if the output was sucessful

```bash
hdfs dfs -ls /usr/local/hadoop
```
 
 You should find a testk82-<timestamp directory>