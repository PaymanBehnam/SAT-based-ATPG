package gateparser;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.*;

/**
 *
 * @author Onymous
 */
class Node{
    int type=-1;
    ArrayList<String> inputs=new ArrayList<String>();
    String output="";
    String typeName="";
}

class Fault{
    int type=-1;
    String wire1="";
    String wire2="";
    boolean value=false;
}

public class GateParser {
    static int OR=6,AND=4,XOR=9,XNOR=10,NOT=8,BUF=11,INPUT=3,OUTPUT=2,NAND=5,NOR=7,DFF=12,vdd=13,LUT=14, gnd=15;
    static String[] gateTypes={"","","OUTPUT","INPUT","AND","NAND","OR","NOR","NOT","XOR","XNOR","BUF","DFF","vdd","LUT","gnd"};
    
    public static void makeABCScript(String scriptName,int rollNum,String fin,String fout){
        try{
            Writer so=new OutputStreamWriter(new FileOutputStream(new File(scriptName)));
            so.write("read_bench "+fin+"\n");
            //so.write("fraig"+"\n");
            so.write("frames -Fi "+rollNum+"\n");
            so.write("write_bench "+fout+"\n");
            so.close();
        }catch(Exception e){System.out.println("error2");System.exit(1);}
        
    }
     public static void makeABCScript2(String scriptName,int rollNum,String fin,String fout){
        try{
            Writer so=new OutputStreamWriter(new FileOutputStream(new File(scriptName)));
            so.write("read_bench "+fin+"\n");
            so.write("strash"+"\n");
            so.write("logic "+"\n");
            so.write("write_bench "+fout+"\n");
            so.close();
        }catch(Exception e){System.out.println("error9");System.exit(1);}
        
    }
    public static void makeABCScript3(String scriptName,int rollNum,String fin,String finter,String fout){
        try{
            Writer so=new OutputStreamWriter(new FileOutputStream(new File(scriptName)));
            so.write("read_bench "+fin+"\n");
            so.write("strash"+"\n");
            //so.write("frames -Fi "+rollNum+"\n");
            so.write("logic "+"\n");
            so.write("miter "+fin+" "+ finter+"\n");
           // so.write("read_eqn "+fin+"\n");
            //so.write("fraig"+"\n");
            //so.write("strash"+"\n");
            so.write("write_cnf "+fout+"\n");
            so.close();
        }catch(Exception e){
            System.out.println(e.getMessage());System.exit(1);
        }
        
    }
    public static void main(String[] args) {
        ArrayList<Node> nodes=gatesParse("gate.in");
        ArrayList<Fault> faults=faultParse("faults.in");
        makeABCScript("abccmd.in",16,"gate.in","unrolledgate.in");
        String cmd="abc  -F abccmd.in";
        
        try{
            Process pr=Runtime.getRuntime().exec(cmd);
            pr.waitFor();
        }catch(Exception e){
            System.out.println("error3");System.exit(1);
        }
        
        makeABCScript2("abccmd2.in",16,"unrolledgate.in","unrolledgatein4miter.bench");
        String cmd2="abc  -F abccmd2.in";
        
        try{
            Process pr=Runtime.getRuntime().exec(cmd2);
            pr.waitFor();
        }catch(Exception e){
            System.out.println(e.getMessage());System.exit(1);
        }
        
        
        
        
        
        
        ArrayList<Node> unrolledNodes=gatesParse("unrolledgate.in");
        Iterator<Fault> it=faults.iterator();
        while(it.hasNext()){
            
            Fault fl=it.next();
//            if(fl.type==2)
//                continue;
            ArrayList<Node> faultyNodes=cloneNodes(nodes);
            ArrayList<Node> tmpNodes=new ArrayList<Node>();
            
            
            
            Iterator<Node> it2=faultyNodes.iterator();
            while(it2.hasNext()){
                Node n=it2.next();
                if(n.type==INPUT){//inserting buffres in inputs
                        Node n2=new Node();
                        n2.inputs.add(n.inputs.get(0));
                        n2.output=n.inputs.get(0)+"_WFCIRCUIT";
                        n2.type=BUF;
                        n2.typeName="BUF";
                        tmpNodes.add(n2);
                        continue;
                }
                n.output=n.output+"_WFCIRCUIT";//renaming faulty output
                for(int h=0;h<n.inputs.size();h++){
                    String s=n.inputs.get(h);
                    n.inputs.set(h,s+"_WFCIRCUIT");//renaming faulty input
                }
            }
            faultyNodes.addAll(tmpNodes);
            
            
            it2=faultyNodes.iterator();
            while(it2.hasNext()){
                Node n=it2.next();
                if(fl.type==2){//when we have xx->xx fault
                    if(n.output.compareToIgnoreCase(fl.wire2+"_WFCIRCUIT")!=0)
                        continue;
                }
                for(int h=0;h<n.inputs.size();h++){
                    String s=n.inputs.get(h);
                    if(s.compareToIgnoreCase(fl.wire1+"_WFCIRCUIT")==0){
                        n.inputs.set(h,fl.wire1 +"_FSA_WFCIRCUIT");
                    }
                }
            }
            Node not=new Node();
            not.type=NOT;
            not.typeName="NOT";
            not.inputs.add(fl.wire1+"_WFCIRCUIT");
            not.output=fl.wire1+"_NOTF"+"_WFCIRCUIT";
            faultyNodes.add(not);
            if(fl.value==true){
                Node and=new Node();
                and.type=AND;
                and.typeName="AND";
                and.inputs.add(fl.wire1+"_WFCIRCUIT");
                and.inputs.add(fl.wire1+"_NOTF"+"_WFCIRCUIT");
                and.output=fl.wire1+"_FSA_WFCIRCUIT";
                faultyNodes.add(and);
            }else{
                Node or=new Node();
                or.type=OR;
                or.typeName="OR";
                or.inputs.add(fl.wire1+"_WFCIRCUIT");
                or.inputs.add(fl.wire1+"_NOTF"+"_WFCIRCUIT");
                or.output=fl.wire1+"_FSA_WFCIRCUIT";
                faultyNodes.add(or);
            }
            writeNodes(faultyNodes,"faults\\fault_"+fl.wire1+"_"+fl.wire2+"_"+fl.value+ ".out");
            makeABCScript("abccmd3.in",16,"faults\\fault_"+fl.wire1+"_"+fl.wire2+"_"+fl.value+ ".out","unrolled_faults\\unrolledfault_"+fl.wire1+"_"+fl.wire2+"_"+fl.value+ ".out");
            cmd="abc -F abccmd3.in";
            try{
                Process pr=Runtime.getRuntime().exec(cmd);
                pr.waitFor();
                //Runtime.getRuntime()
            }catch(Exception e){
                System.out.println("e);System.exit(1)");
                        }

            
//            writeNodes(faultyNodes,"unrolled_faults\\unrolledfault_"+fl.wire1+"_"+fl.wire2+"_"+fl.value+ ".out");//forgotten
//            if(fl.wire1.compareToIgnoreCase("G56")==0){
//                System.out.println("r1rr"+"unrolledfault_"+fl.wire1+"_"+fl.wire2+"_"+fl.value+ ".out");
//            }
            ArrayList<Node> unrolledFaultyNodes=gatesParse("unrolled_faults\\unrolledfault_"+fl.wire1+"_"+fl.wire2+"_"+fl.value+ ".out");
            
            //ArrayList<Node> faultyNodes2=cloneNodes(nodes);
            ArrayList<Node> tmpNodes2=new ArrayList<Node>();
            
            it2=unrolledFaultyNodes.iterator();
            Iterator<Node> it3 ;
            while(it2.hasNext()){
                Node n=it2.next();
                if(n.output.length()>0 && n.output.charAt(0)=='n')
                {
                    n.output="WFCIRCUIT_" + n.output;
                    it3 = unrolledFaultyNodes.iterator();
                    while( it3.hasNext())
                    {
                        Node n3 = it3.next() ;
                        for(int h=0;h< n3.inputs.size();h++)
                        {
                            String s= n3.inputs.get(h);
                            if(s.charAt(0)=='n')
                            n3.inputs.set(h,"WFCIRCUIT_" + s);
                        }
                    }
                }
            }
            it2=unrolledFaultyNodes.iterator();
            while(it2.hasNext()){
                Node n=it2.next();
                if(n.type==INPUT && n.inputs.get(0).contains("_WFCIRCUIT")){//inserting buffres in inputs
                        Node n2=new Node();
                        n2.inputs.add(n.inputs.get(0).replace("_WFCIRCUIT", ""));
                        n2.output=n.inputs.get(0);
                        n2.type=BUF;
                        n2.typeName="BUF";
                        tmpNodes2.add(n2);
                        n.inputs.set(0,  n.inputs.get(0).replace("_WFCIRCUIT", "") );
                        continue;
                }
                if(n.type==OUTPUT && n.inputs.get(0).contains("_FSA_WFCIRCUIT")){//inserting buffres in inputs
                        Node n2=new Node();
                        n2.output = n.inputs.get(0).replace("_FSA_WFCIRCUIT" , "") ;
                        n2.inputs.add( n.inputs.get(0)) ;
                        n2.type=BUF;
                        n2.typeName="BUF";
                        tmpNodes2.add(n2);
                        n.inputs.set(0, n.inputs.get(0).replace("_FSA_WFCIRCUIT", "")) ;
                        continue;
                }
                //replaced output with inputs.get(0) for both n and n2
                if(n.type==OUTPUT && n.inputs.get(0).contains("_WFCIRCUIT")){//inserting buffres in inputs
                        Node n2=new Node();
                        n2.output = n.inputs.get(0).replace("_WFCIRCUIT" , "") ;
                        n2.inputs.add( n.inputs.get(0)) ;
                        n2.type=BUF;
                        n2.typeName="BUF";
                        tmpNodes2.add(n2);
                        n.inputs.set(0, n.inputs.get(0).replace("_WFCIRCUIT", "")) ;
                        continue;
                }
                
            }
                unrolledFaultyNodes.addAll(tmpNodes2);
                    
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            //writeNodes(unrolledFaultyNodes,"final_for_mitter\\final_for_mitter"+fl.wire1+"_"+fl.wire2+"_"+fl.value+ "beforeclone.out");
            ArrayList<Node> finalCircuit=cloneNodes(unrolledFaultyNodes);
            //appendNodes(finalCircuit,unrolledFaultyNodes);
            //writeNodes(finalCircuit,"final\\final_"+fl.wire1+"_"+fl.wire2+"_"+fl.value+ ".out");
           writeNodes(finalCircuit,"final_for_mitter\\final_for_mitter"+fl.wire1+"_"+fl.wire2+"_"+fl.value+ ".bench");
            makeABCScript3("abccmd4.in",16,"final_for_mitter\\final_for_mitter"+fl.wire1+"_"+fl.wire2+"_"+fl.value+ ".bench","unrolledgatein4miter.bench","CNF\\cnf_"+fl.wire1+"_"+fl.wire2+"_"+fl.value+ ".out");
            String cmd3="abc -F abccmd4.in";
            try{
                Process pr=Runtime.getRuntime().exec(cmd3);
                pr.waitFor();
                Process pr2=Runtime.getRuntime().exec(cmd3);
                pr2.waitFor();
                System.out.println( Integer.toString(pr.exitValue())  +  pr.getErrorStream());
                int ret = pr.getErrorStream().read() ;
                int y = 11 ;
            }catch(Exception e){
                System.out.println(e.getMessage());
                System.exit(1);
            }

            

        }
        
    }
    
    public static void appendNodes(ArrayList<Node> src,ArrayList<Node> app){
        Iterator<Node> it=app.iterator();
        
        
        
        while(it.hasNext()){
            Node n=it.next();
            if(n.type==INPUT || n.type==OUTPUT)
                continue;
            src.add(n);
        
        }
        
        /*Node or=new Node();
        or.type=OR;
        it=src.iterator();
        while(it.hasNext()){
            Node n=it.next();
            if(n.type==OUTPUT){
                n.type=XOR;
                n.typeName="XOR";
                n.output=n.inputs.get(0)+"_XOR";
                n.inputs.add(n.inputs.get(0)+"_WFCIRCUIT");
                or.inputs.add(n.output);
            }
        }
        
        or.output="EVENTUAL_OUT";
        or.typeName="OR";
        src.add(or);
        
        Node outfinal=new Node();
        outfinal.type=OUTPUT;
        outfinal.typeName="OUTPUT";
        outfinal.inputs.add("EVENTUAL_OUT");
        src.add(0,outfinal);
    */
    }
    
    

    public static ArrayList<Node> cloneNodes(ArrayList<Node> nn){
        Iterator<Node> it=nn.iterator();
        ArrayList<Node> cnn=new ArrayList<Node>();
        while(it.hasNext()){
            Node n=it.next();
            Node cn=new Node();
            cn.output=new String(n.output);
            cn.type=n.type;
            cn.typeName=new String(n.typeName);
            
            Iterator<String> it2=n.inputs.iterator();
            while(it2.hasNext()){
                cn.inputs.add(new String(it2.next()));
            }
            cnn.add(cn);
        }
        return cnn;
    }
    
public static void writeNodes(ArrayList<Node> nodes,String foutname){
    try{
        Writer fout=new OutputStreamWriter(new FileOutputStream(new File(foutname)));
        Iterator<Node> it4=nodes.iterator();
        while(it4.hasNext()){
            Node n=it4.next();
            String ss=new String("");
            if(n.type==INPUT || n.type==OUTPUT){
                //ss=gateTypes[n.type]+"("+n.inputs.get(0) +")";
                ss=n.typeName+"("+n.inputs.get(0) +")";
            }else{
                //ss=n.output+" = "+gateTypes[n.type]+"(";
                if(n.type==vdd ||n.type==gnd){
                    ss=n.output+" = "+n.typeName;
                }else{
                    ss=n.output+" = "+n.typeName+"(";
                    for(int e=0;e<n.inputs.size()-1;e++){
                        ss=ss+n.inputs.get(e)+", ";
                    }
                    if(n.inputs.size()>0)
                        ss=ss+n.inputs.get(n.inputs.size()-1)+")";
                    else
                        ss=ss+")";
                }
                //System.out.println();
            }
            fout.write(ss+"\n");
        }
        fout.close();
    }catch(Exception e){System.out.println("error1"+e);System.exit(1);}

}
    public static ArrayList<Fault> faultParse(String fname){
        File file = new File(fname);
        int ch;
        StringBuffer strContent = new StringBuffer("");
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(file);
            while ((ch = fin.read()) != -1){
                strContent.append(((char)ch));
            }
            fin.close();
        } catch (Exception e) {;}
        String str=strContent.toString();
        Pattern tag = Pattern.compile(".*?/\\d|test.*? faults detected");
        Matcher mtag = tag.matcher(str);
        int j=0;
        ArrayList<Fault> allFaults=new ArrayList<Fault>();
        while (mtag.find()){
            j=j+1;
            Fault n=new Fault();
            Pattern tagInner = Pattern.compile("[/\\-\\>]+");
            String tmpstr=mtag.group();
            String[] ttt=tagInner.split(tmpstr.trim());
            if(ttt.length<2)
                continue;
            n.wire1=ttt[0].trim();
            if(ttt.length>2){
                n.type=2;
                n.wire2=ttt[1].trim();
                int vv=new Integer(ttt[2].trim()).intValue();
                if(vv==1)
                    n.value=true;
                else
                    n.value=false;
            }else{
                n.type=1;
                int vv=new Integer(ttt[1].trim()).intValue();
                if(vv==1)
                    n.value=true;
                else
                    n.value=false;
            }
            allFaults.add(n);
        }
        return allFaults;
    }
    
    public static ArrayList<Node> gatesParse(String fname){
        File file = new File(fname);
        int ch;
        StringBuffer strContent = new StringBuffer("");
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(file);
            while ((ch = fin.read()) != -1){
                strContent.append(((char)ch));
            }
            fin.close();
        } catch (Exception e) {;}
        String str=strContent.toString();
        Pattern tag = Pattern.compile("(#.*?)|(OUTPUT\\(.*?\\))|(INPUT\\(.*?\\))|(.*? = AND\\(.*?\\))|(.*? = NAND\\(.*?\\))|(.*? = OR\\(.*?\\))|(.*? = NOR\\(.*?\\))|(.*? = NOT\\(.*?\\))|(.*? = XOR\\(.*?\\))|(.*? = XNOR\\(.*?\\))|(.*? = BUF\\(.*?\\))|(.*? = DFF\\(.*?\\))|(.*? = LUT 0x.*?\\(.*?\\))|(.*? = vdd)|(.*? = gnd)");
        Matcher mtag = tag.matcher(str);
        int j=0;
        ArrayList<Node> allNodes=new ArrayList<Node>();
        while (mtag.find()){
            j=j+1;
            for(int i=2;i<=mtag.groupCount();i++){
                if(mtag.group(i)!=null){
                    //System.out.println(i+":"+mtag.group());
                    Node n=new Node();
                    Pattern tagInner = Pattern.compile("[,=\\(\\)]+");
                    String tmpstr=mtag.group();
                    String[] ttt=tagInner.split(tmpstr.trim());
                    if(i>=4){
                        n.output=ttt[0].trim();
                        n.type=getType(ttt[1].trim());
                        n.typeName=ttt[1].trim();
                        for(int k=2;k<ttt.length;k++){
                            n.inputs.add(ttt[k].trim());
                        }
                        allNodes.add(n);
                    }
                    if(i==INPUT || i==OUTPUT){
                        n.inputs.add(ttt[1].trim());
                        n.typeName=ttt[0].trim();
                        n.type=getType(ttt[0].trim());
                        allNodes.add(n);
                    }
                    
                
                    break;
                }
            }
        }
        return allNodes;
    }

   
    public static int getType(String s){
        
        if(s.compareToIgnoreCase("vdd")==0){
            return vdd;
        }
        if(s.compareToIgnoreCase("gnd")==0){
            return gnd;
        }
        if(s.compareToIgnoreCase("XNOR")==0){
            return XNOR;
        }
        if(s.compareToIgnoreCase("DFF")==0){
            return DFF;
        }
        if(s.compareToIgnoreCase("INPUT")==0){
            return INPUT;
        }
        if(s.compareToIgnoreCase("OUTPUT")==0){
            return OUTPUT;
        }
        if(s.compareToIgnoreCase("AND")==0){
            return AND;
        }
        if(s.compareToIgnoreCase("NAND")==0){
            return NAND;
        }
        if(s.compareToIgnoreCase("NOR")==0){
            return NOR;
        }
        if(s.compareToIgnoreCase("OR")==0){
            return OR;
        }
        if(s.compareToIgnoreCase("XOR")==0){
            return XOR;
        }
        //if(s.compareToIgnoreCase("NOT")==0){
         //   return NOT;
        //}
        if(s.compareToIgnoreCase("BUF")==0){
            return BUF;
        }
        if(s.compareToIgnoreCase("NOT")==0){
            return NOT;
        }
        return -1;
    }
}

