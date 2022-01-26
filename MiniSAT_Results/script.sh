uncov=0
iter=0
t_vars=0
t_restarts=0
t_clauses=0
t_conflicts=0
t_memory=0
t_times=0
cd CNF
echo 4.0/10.0 | bc -l
files=$(ls *.out)
rm -r ./satorunsat
rm -r ./report_satorunsat 
mkdir satorunsat
mkdir report_satorunsat
for file in $files
do
echo $file
../minisat $file  -rinc=1.5 -phase-saving=1 -rnd-freq=0.02 satorunsat/$file.cnf >> report_satorunsat/$file.cnf.txt
#sats=$(awk -F 'SAT' 'BEGIN{n=0} {n=1} {print n}' $file.cnf)
vars=$(awk   '$4=="variables:" { print $5 }'  report_satorunsat/$file.cnf.txt  )
clauses=$(awk   '$4=="clauses:" { print $5 }'  report_satorunsat/$file.cnf.txt )
restarts=$(awk   '$1=="restarts" { print $3 }'  report_satorunsat/$file.cnf.txt )
conflicts=$(awk   '$1=="conflicts" { print $3 }'  report_satorunsat/$file.cnf.txt )
decisions=$(awk   '$1=="decisions" { print $3 }'  report_satorunsat/$file.cnf.txt )
memory=$(awk   '$1=="Memory" { print $4 }'  report_satorunsat/$file.cnf.txt )
times=$(awk   '$1=="CPU" { print $4 }'  report_satorunsat/$file.cnf.txt )
echo vars=$vars claues=$clauses restarts=$restarts conflicts=$conflicts decisions=$decisions memory=$memory time=$times
unsats=$(grep -o  UNSAT satorunsat/$file.cnf | wc -w)
t_vars=$(expr $t_vars + $vars)
t_clauses=$(expr $t_clauses + $clauses)
t_restarts=$(expr $t_restarts + $restarts)
t_conflicts=$(expr $t_conflicts + $conflicts)
t_decisions=$(expr $t_decisions + $decisions)

t_memory=$(echo $t_memory+$memory | bc -l)
uncov=$(expr $uncov + $unsats)
iter=$(expr $iter + 1) 
done
echo $uncov
echo $iter
echo $t_vars/$iter | bc -l
echo $t_clauses/$iter | bc -l
echo $t_restarts/$iter | bc -l
echo $t_conflicts/$iter | bc -l
echo $t_decisions/$iter | bc -l
echo $t_memory/$iter | bc -l

