# Introduction
This repository contains various artifacts, such as source code, experimental results, and other materials, that supplement our work on the Composite Object Normal Form (CONF).\
&nbsp;&nbsp;&nbsp;&nbsp;Foremost, the repository contains an implementation of the lossless FD-preserving decomposition algorithm (Alg.1 at <kbd>conf/CONF.java</kbd>) we have proposed. Subsequently, we have implemented an additional three algorithms that allow us to compare Alg. 1 with previous state-of-the-art algorithms: Alg. 2(<kbd>conf/CONF_Comp.java</kbd>), Alg. 3(<kbd>conf/CONF_3NF.java</kbd>) and Alg. 4(<kbd>conf/CONF_Basic.java</kbd>). We have also included the code of implementations for other experiments: <kbd>conf/</kbd> and <kbd>additional/</kbd>. For all experimental results, logs and some sql scripts are included, too (<kbd>Artifact/02 - Experiments/</kbd>). Finally, we have also included the proofs of Theorems we could not fit into the paper due to page restrictions <kbd>Artifact/01 - Proofs/</kbd>.\
&nbsp;&nbsp;&nbsp;&nbsp;In the following sections, we describe how our experiments can be reproduced. 
# Preliminaries: Getting databases ready for experiments
> 1. Import 14 datasets as SQL databases
>> We have used MySQL 8.0 as database workbench. Firstly, please create a database called "freeman". Afterwards, import the [14 datasets](https://hpi.de/naumann/projects/repeatability/data-profiling/fds.html) as MySQL databases by setting column names as 0,1,...,n-1 where n is the number of columns in a given dataset. In addition, please create a column named "id" as an auto_increment attribute for each table.
> 2. Import TPC-H benchmark
>> Please visit the [website](https://relational.fit.cvut.cz/dataset/TPCH) and export the TPC-H database as an .sql file. Then, please import the file in your own local MySQL workbench. Under <kbd>Artifact/02 - Experiments/3 - Performance of n-CONF/TPC-H/</kbd> we have included all 22 official SQL queries and refresh functions to be used in our experiments.
>3. Functional dependencies (FDs)
>> For each of the 14 datasets, the atomic closure for the set of FDs that hold on a dataset are given as separate json files in <kbd>Artifact/03 - FD/</kbd>. For the TPC-H benchmark, we have implemented the FDs inside our code.
>4. JDK & JDBC
>> Our code was developed in JAVA. As a consequence, please specify a JDK with version 8 or later. At the moment, we are using JDBC (version 8.0.26) as a connector to MySQL databases.
# Experiments
In line with our paper, our experiments are organized into five sections. For each of them, you can run different code/scripts:
>1. Number of Keys on Real-World Schemata
>> In this experiment, we connected to [the relational data
repository](https://relational.fit.cvut.cz) to check how many uniqueness constraints (primary key plus additional UNIQUE constraints) are i) specified or ii) valid on some public databases. Results are available for a variety of databases, including the numbers on each of the tables in each of the databases <kbd>Artifact/02 - Experiments/1 - Number of Keys on Real-World Schemata/</kbd>. The paper contains only a summary of them.
>2. Why to minimize 3NF schemata?
>> For this experiment, you need to select the lineitem dataset as the dataset source in <kbd>conf/Constant.java</kbd>. Note that lineitem is the biggest table in the TPC-H benchmark. Subsequently, please set some options in the main function of <kbd>conf/RealWorldSelectAndUpdate.java</kbd>, and finally run the class to start this experiments. We have included our experimental results in <kbd>Artifact/02 - Experiments/2 - Why to minimize 3NF schemata/</kbd>
> 3. Performance of n-CONF
>> 3.1 Armstrong relations
>>> The code for generating Armstrong relations for a given set of minimal keys on a given set of attributes is given here: <kbd>conf/KeyNumExp.java</kbd>. Parameters can be set by initializing some variables in the main function. Subsequently, you can run the class to execute experiments. For our experimental results and graphs, please see <kbd>Artifact/02 - Experiments/3 - Performance of n-CONF/Armstrong relations/</kbd>.

>> 3.2 TPC-H benchmark
>>> For the TPC-H benchmark experiments, we preselected some minimal keys in the function <kbd>get_table_uniques_from_TPCH()</kbd> of <kbd>additional/KeyNumExpOnTPCBenchmark.java</kbd>. These keys were included in the set of minimal keys mined from the dataset. To run the code, you simply need to set some parameters in the main funtion and then start. Our experimental results and statistics are included in <kbd>Artifact/02 - Experiments/3 - Performance of n-CONF/TPC-H/</kbd>.
> 4. Performance of Algorithms
>> 4.1 Real-world
>>> Before running these experiments, set some parameters in <kbd>conf/Constant.java</kbd> to access some datasets, and to turn enable/disable some options. Afterwards, you can run <kbd>conf/Main.java</kbd> to start the experiment. Our results are shown in <kbd>Artifact/02 - Experiments/4 - Performance of Algorithms/Real-world/</kbd>.

>> 4.2 Lineitem
>>> The performance comparison of Alg. 1 over Alg. 2 is done in <kbd>additional/SubschemaPerfExp.java</kbd>. Before starting this experiment, create a new file based on the sub-schemata of the decomposition result from Experiment 4.1 for the lineitem dataset that are in BCNF. Then configure the main function and start. Our results are shown in <kbd>Artifact/02 - Experiments/4 - Performance of Algorithms/Lineitem/</kbd>.

>> 4.3 RunningExample
>>> For experiments with our running example, as introduced in our paper, we have placed our experimental results, schema definitions, stored procedures, and SQL scripts for update and query operations in <kbd>Artifact/02 - Experiments/4 - Performance of Algorithms/RunningExample/</kbd>. These can be repeated by creating a database for each of the two decompositions (3NF vs 2-CONF), populating them with different numbers of records, and then running the operations. 
> 5. PTIME BCNF
>> In this experiment, we analyzed the cost of conducting BCNF decompositions that are guaranteed to be in polynomial deterministic time in the input. We implemented Tsou and Fischer's classical PTIME lossless BCNF-decomposition algorithm in <kbd>additional/LosslessJoinDecompIntoBCNF.java</kbd>. Before the execution, set up the corresponding dataset information in <kbd>conf/Constant.java</kbd>, and some other variables in the main function of <kbd>additional/LosslessJoinDecompIntoBCNF.java</kbd>. After this you can start the experiment. Finally, you can check the folder <kbd>Artifact/02 - Experiments/5 - PTIME BCNF/</kbd> for our experimental statistics and logs.
