import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Index {

	public Index() {
	}
	/**
	 * These Hashmaps are for my termMap, and my vector maps for docs and queries,
	 * and my result map
	 * 
	 */
	public static HashMap<String,TFPAIR> wordMap = new HashMap<String,TFPAIR>();
	public static HashMap<String,HashMap<String, Double>> docTFMap = new HashMap<String,HashMap<String, Double>>();
	public static HashMap<String,HashMap<String, Double>> qTFMap = new HashMap<String,HashMap<String, Double>>();
	public static HashMap<String,HashMap<String, Double>> Result = new HashMap<String,HashMap<String, Double>>();
	/*
	 * This data structure is a Map of DOCID, NORM, where NORM is the 
	 * normalization factor I will need for normalization
	 */
	public static HashMap<String,Double> docNormFactorMap = new HashMap<String,Double>();
	public static HashMap<String,Double> idfMap = new HashMap<String,Double>();
	
	/**
	 * 
	 * pair of DOCID and position for a word	  
	 */
	public static class DOCPAIR {
		public String DOCID = "";
		public int POS = 0;
		
		public DOCPAIR(String docid, int pos){
			this.DOCID = docid;
			this.POS = pos;
		}
		@Override
		public String toString() {
			return "[" + this.DOCID + "," + this.POS + "]";
		}
	}
	/**
	 * 
	 * pair of a Double term frequency score, and a list of occurrences of word
	 */
	public static class TFPAIR {
		public Double TF = 0.0;
		public ArrayList<DOCPAIR> OCC = new ArrayList<DOCPAIR>();
				
		public TFPAIR(Double tf, ArrayList<DOCPAIR> occ){
			this.TF = tf;
			this.OCC = occ;
		}
		@Override
		public String toString() {
			return "[" + this.TF + "," + this.OCC.toString() + "]";
		}
	}
	
	
	/*
	 * returns DOCID of a String, as a String
	 * 
	 */
	public static String getDOCID(String doc) {
		String docDelim = "<DOCNO>";
		String docDelimClose = "</DOCNO>";
		//split document into lines
		String[] lineArray = doc.split("\n");			
		for (String line: lineArray) {
			if (line.contains(docDelim)) {			
				//remove tags from line
				String lineclean = line.replace(docDelim, "");
				String lineclean2 = lineclean.replace(docDelimClose, "");				
				return lineclean2.trim();
			} else {
				continue;
			}		
		}
		//docid not found
		return "NOTFOUND-check getDOCID()";
	}
	/*
	 * @return a String, as an Array of Strings after Indexing the words in it 
	 */
	public static ArrayList<String> putWords(String docid, String line, int POS) throws IOException {
		String lineclean3 = line.trim();
		ArrayList<String> words = tokenizer(lineclean3);
		int pos = POS;
		
		//check if need to make a new docmap
		if(docTFMap.containsKey(docid)==false) {
			HashMap<String,Double> newMap = new HashMap<String,Double>();
			docTFMap.put(docid, newMap);
		}
		HashMap<String, Double> thisdocmap = docTFMap.get(docid);
		
		for (String word : words) {
			if (word.length()<1) {
				continue;
			}
			
			//no word found, create wordMap entry
			if (wordMap.containsKey(word)==false) {
				ArrayList<DOCPAIR> pairList = new ArrayList<DOCPAIR>();
				DOCPAIR pair = new DOCPAIR(docid,pos++);
				pairList.add(pair);
				TFPAIR pairB =  new TFPAIR(1.0,pairList);
				wordMap.put(word, pairB);
			} else {
				Index.TFPAIR tfpair = wordMap.get(word);
				double tf = tfpair.TF;
				tf++;
				//get docpairlist and add to it
				ArrayList<Index.DOCPAIR> docpairlist = tfpair.OCC;
				Index.DOCPAIR newdocpair = new DOCPAIR(docid, pos++);
				docpairlist.add(newdocpair);
				//add new tfpair to map
				Index.TFPAIR newtfpair = new TFPAIR(tf, docpairlist);
				wordMap.put(word, newtfpair);
			}
			
			//now fill docMap
			if(thisdocmap.containsKey(word)==false) {
				thisdocmap.put(word, 1.0);
			} else {
				double tf = thisdocmap.get(word);
				thisdocmap.put(word, ++tf);
			}
			
		}//end a word
		//update doctfmap
		docTFMap.put(docid, thisdocmap);
		return words;		
	}		

	
	/*
	 * @return Head of a String, as an Array of Strings. Indexes words in HEAD. 
	 */
	public static ArrayList<String> getHEAD(String docid, String line) throws IOException {
		String headDelim = "<HEAD>";
		String headDelimClose = "</HEAD>";
		if (line.contains(headDelim)) {			
			//remove tags from line
			String lineclean = line.replace(headDelim, "");
			String lineclean2 = lineclean.replace(headDelimClose, "");
			//removed the Eds tag
			String eds = "Eds:";
			String headclean = lineclean2.replace(eds, "");
			String lineclean3 = headclean.trim();
			ArrayList<String> words = tokenizer(lineclean3);
			int pos = 1;
			for (String word : words) {
				//no word found, create wordMap entry
				if (wordMap.containsKey(word)==false) {
					ArrayList<DOCPAIR> pairList = new ArrayList<DOCPAIR>();
					DOCPAIR pair = new DOCPAIR(docid,pos++);
					pairList.add(pair);
					TFPAIR pairB =  new TFPAIR(1.0,pairList);
					wordMap.put(word, pairB);
				} else {
					Index.TFPAIR tfpair = wordMap.get(word);
					double tf = tfpair.TF;
					tf++;
					//get docpairlist and add to it
					ArrayList<Index.DOCPAIR> docpairlist = tfpair.OCC;
					Index.DOCPAIR newdocpair = new DOCPAIR(docid, pos++);
					docpairlist.add(newdocpair);
					//add new tfpair to map
					Index.TFPAIR newtfpair = new TFPAIR(tf, docpairlist);
					wordMap.put(word, newtfpair);
				}
			}
			return words;
		} else {
			return null;
		}		
	}


	/*
	 * this function tokenizes a string of words
	 */
	public static ArrayList<String> tokenizer(String input) throws IOException {
		//first i import my stopwords
		List<String> stopwords = Files.readAllLines(Paths.get("src/stoplist.txt"));		
		input = input.replaceAll("[^a-zA-Z ]", "");
		ArrayList<String> words = Stream.of(input.toLowerCase().split(" "))
	            .collect(Collectors.toCollection(ArrayList<String>::new));
	    words.removeAll(stopwords);
		return words;
	}
	/*
	 * This is my main function that does my indexing.
	 * 
	 * @returns the # of documents indexed
	 */
	public static void index() throws IOException {
		//first I need to get my Array of Files
		String folderPath = "src/ap89_collection";
		File folder = new File(folderPath);
		File[] directoryArray = folder.listFiles();
		
		//for every file in my directory
		for(File file: directoryArray) {
			//get path into stream
			String path = file.getPath();
			Path myPath = Paths.get(path);
			Stream<String> streamOfStrings;
			String fileString = "";
			
			//stream from file to get a String from each file			
			streamOfStrings = Files.lines(myPath, StandardCharsets.ISO_8859_1);
			StringBuilder fileStringBuilder = new StringBuilder();
			streamOfStrings.forEach(s -> fileStringBuilder.append(s).append("\n"));
			fileString = fileStringBuilder.toString();
			streamOfStrings.close();
	
			//Now I split by documents
			String docRX = "<DOC>";
			String[] DocArray = fileString.split(docRX);
			for(String document: DocArray ) {
				String DOCID = getDOCID(document);
				//check if need to make a new docmap
				if(docTFMap.containsKey(DOCID)==false) {
					HashMap<String,Double> newMap = new HashMap<String,Double>();
					docTFMap.put(DOCID, newMap);
				}
								
				//split document into lines
				String[] lineArray = document.split("\n");
				for(int i=0;i<lineArray.length-1;i++) {
					String line = lineArray[i];
					getHEAD(DOCID,line);
					if (line.contains("<TEXT>")){
						int POS = 1;
						// stops when delim or hits end of doc
						while (lineArray[i].contains("</TEXT>") == false && i+1<lineArray.length) {
							String textLine = lineArray[++i];
							putWords(DOCID, textLine, POS);							
						}
					}
					
				}//end <TEXT>
				//calculate normalization factor
				double norm = 0.0;
				HashMap<String, Double> thisdocmap = docTFMap.get(DOCID);
				for (String term: thisdocmap.keySet()) {
					double tf = thisdocmap.get(term);
					tf = java.lang.Math.pow(tf, 2);
					norm +=tf;
				}
				norm = java.lang.Math.sqrt(norm);
				docNormFactorMap.put(DOCID, norm);
			}//end document
		}
	}
	
	/*
	 * this function is for searching the querys in query_desc51-100.txt
	 * 
	 */
	public static void map51to100() throws IOException{
		String filepath = "src/query_desc.51-100.short.txt";
		Path myPath = Paths.get(filepath);
		//first i get my file into a stream of Lines
		Stream<String> streamOfStrings;
		streamOfStrings = Files.lines(myPath, StandardCharsets.ISO_8859_1);
		StringBuilder fileStringBuilder = new StringBuilder();
		streamOfStrings.forEach(s -> fileStringBuilder.append(s).append("\n"));
		String queries = fileStringBuilder.toString();
		streamOfStrings.close();
		
		double totaldocs = docTFMap.size();
		//split into lines
		String[] queryArray = queries.split("\n");
		for (String line: queryArray) {
			HashMap<String,Double> queryTF = new HashMap<String,Double>();
			//split into words
			ArrayList<String> wordArray = tokenizer(line);
			for (String word: wordArray) {
				if (word.length() < 1) {
					continue;
				}
				//first build tfmap
				if(queryTF.containsKey(word) == false) {
					queryTF.put(word, 1.0);

				}
				if(queryTF.containsKey(word) == true) {
					double count = queryTF.get(word);
					count++;
					queryTF.put(word, count);

				}
				if (idfMap.containsKey(word)) {
					continue;
				}
				double df = 0.0;
				ArrayList<String> idfList = new ArrayList<String>();
				if (wordMap.containsKey(word)==false) {
					idfMap.put(word, 0.0);
					continue;
				}
				ArrayList<DOCPAIR> occurrenceList = wordMap.get(word).OCC;
				for (DOCPAIR pair: occurrenceList) {
					String thisdocid = pair.DOCID;
					if (idfList.contains(thisdocid)==false) {								
						df++;
						idfList.add(thisdocid);
					}
				}
				double idf = totaldocs / df;
				idf = java.lang.Math.log10(idf);
				idfMap.put(word, idf);
			}//end a word
			
			
			
			qTFMap.put(line, queryTF);
		}//end line
	}
	
	/**
	 * this function loads qrel scores into a string array
	 * @throws IOException 
	 * 
	 */
	public static Boolean mapqrel(String docid,String num) throws IOException {
		String filepath = "src/qrels.adhoc.51-100.AP89.txt";
		Path myPath = Paths.get(filepath);
		//first i get my file into a stream of Lines
		Stream<String> streamOfStrings;
		streamOfStrings = Files.lines(myPath, StandardCharsets.ISO_8859_1);
		StringBuilder fileStringBuilder = new StringBuilder();
		streamOfStrings.forEach(s -> fileStringBuilder.append(s).append("\n"));
		String queries = fileStringBuilder.toString();
		streamOfStrings.close();
		
		//split into lines
		String[] queryArray = queries.split("\n");
		for(String line: queryArray) {
			if (line.startsWith(num) && line.contains(docid)){
				if(line.endsWith("1")){
					return true;
				}
			}			
		}
		return false;
	}
	/**
	 * this function actually runs queries51to100
	 * @throws IOException 
	 */
	public static void run51to100() throws IOException {
		for (String query: qTFMap.keySet()) {
			System.out.println("running query " + query.substring(0, 2) + "...");
			HashMap<String, Double> qvector = qTFMap.get(query);
					
			HashMap<String,Double> resultmap = new HashMap<String,Double>();
			
			for(String docid: docTFMap.keySet()) {
				HashMap<String, Double> docvector = docTFMap.get(docid);
				double norm = docNormFactorMap.get(docid);
				double docscore = 0.0;
				//calculate scores
				for (String word: qvector.keySet()) {
					if (wordMap.containsKey(word)==false) {
						continue;
					}
					if(docvector.containsKey(word)) {
						double doctf = docvector.get(word);
						doctf = 1 + java.lang.Math.log(doctf);
						doctf = doctf / norm;
						
						double qtf = qvector.get(word);
						qtf = 1 + java.lang.Math.log(qtf);
	
						double idf = idfMap.get(word);
						double qtfidf = qtf *idf;
						docscore += qtfidf * doctf;
						
					}
				}
				resultmap.put(docid, docscore);
				
			}
			Result.put(query, resultmap);
		}
	}
	
	
	/**
	 * Run the tests in this class.
	 * 
	 * @param args the program arguments
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		index();
		System.out.println("Total Unique Words: " + wordMap.size());		
		//test docTFMap
		System.out.println("Total Documents in docTFMAP: " + docTFMap.keySet().size());
						
		map51to100();
		System.out.println("Total queries in qTFMAP: " + qTFMap.keySet().size());

		
		run51to100();
		BufferedWriter writer = new BufferedWriter(new FileWriter("src/RESULTS.txt"));
		for (String q: Result.keySet()) {
			System.out.println(q);
			String num = q.substring(0, 2);
			
			HashMap<String, Double> resultmap = Result.get(q);
			LinkedHashMap<String, Double> sortedMap = new LinkedHashMap<>();
			//now i sort my map 
			resultmap.entrySet()
			    .stream()
			    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
			    .forEachOrdered(x -> sortedMap.put(x.getKey(), x.getValue()));
			
			writer.write(q + "\n");
			//print top k which is 10
			int k = 50;
			double p = 0.0;
			for (String docid: sortedMap.keySet()) {
				if (k < 1 ) {
					break;
				}
				Boolean isRel = mapqrel(docid, num);
				if (isRel) {
					p++;
				}
				System.out.println(docid + " is relevant?: " + isRel);
				writer.write(docid + " is relevant?: " + isRel + "\n");
				k--;
			}
			p = p /50;
			System.err.println("Query #: " + num + "   Precision @50: " + p);
			writer.write("Query #: " + num + "   Precision @50: " + p  + "\n");
		}
	writer.close();	
	}
}
