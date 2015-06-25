package edu.ucsf.rbvi.stEMAP.internal.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class StructureMap {
	String pdbId = null;
	String pdbFile = null;
	boolean usePDBFile = false;
	String chimeraCommands = "";
	Map<String, String> chainMap = null;
	Map<String, List<String>> duplicateChainMap = null;
	Map<String, Object> rinParameters = null;
	double positiveCutoff = 1.0;
	double negativeCutoff = -2.0;

	public StructureMap(File stFile) throws IOException, FileNotFoundException {
		pdbId = null;
		chimeraCommands = "";
		chainMap = new HashMap<>();
		duplicateChainMap = new HashMap<>();
		rinParameters = new HashMap<String, Object>();

    FileReader reader = new FileReader(stFile);

    try {
      JSONParser jsonParser = new JSONParser();
      JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);

      if (jsonObject.containsKey("PDBId"))
				pdbId = (String)jsonObject.get("PDBId");
      if (jsonObject.containsKey("UsePDBFile"))
				usePDBFile = (Boolean)jsonObject.get("UsePDBFile");
      if (jsonObject.containsKey("PDBFile"))
				pdbFile = (String)jsonObject.get("PDBFile");

			if (pdbId == null && usePDBFile == false && pdbFile == null)
        throw new RuntimeException("No pdb identifier in file");

			if (pdbFile != null) 
				usePDBFile = true;

      if (jsonObject.containsKey("ChimeraCommands"))
				chimeraCommands = (String)jsonObject.get("ChimeraCommands");

      if (!jsonObject.containsKey("ChainMap"))
        throw new RuntimeException("No ChainMap in file");

			JSONObject chains = (JSONObject)jsonObject.get("ChainMap");
			
      for (Object obj: chains.keySet()) {
				if (chains.get(obj) instanceof List) {
					List<?> objList = (List<?>)chains.get(obj);
					List<String> chainList = new ArrayList<>();
					String primaryChain = null;
					for (Object o: objList) {
						if (primaryChain == null) {
							primaryChain = (String)o;
							chainMap.put((String)obj, (String)primaryChain);
						} else {
							chainList.add((String)o);
						}
					}
					if (chainList.size() > 0)
						duplicateChainMap.put(primaryChain, chainList);
				}  else {
					chainMap.put((String)obj, (String)chains.get(obj));
				}
      }

			if (jsonObject.containsKey("PositiveCutoff"))
				positiveCutoff = (Double)jsonObject.get("PositiveCutoff");
			if (jsonObject.containsKey("NegativeCutoff"))
				negativeCutoff = (Double)jsonObject.get("NegativeCutoff");

			if (jsonObject.containsKey("RINParameters")) {
				JSONObject rinParams = (JSONObject)jsonObject.get("RINParameters");
				for (Object obj: rinParams.keySet()) {
					rinParameters.put((String)obj, rinParams.get(obj).toString());
				}
			}

    }
    catch (ParseException pe) {
      throw new RuntimeException("Unable to parse "+stFile+": "+pe);
    }
	}

	public String getPDB() { return pdbId; }
	public boolean usePDBFile() { return usePDBFile; }
	public String getPDBFile() { return pdbFile; }
	public String getChimeraCommands() { return chimeraCommands; }
	public String getPrimaryChain(String key) { 
		return chainMap.get(key); 
	}
	public List<String> getDuplicateChains(String key) { 
		return duplicateChainMap.get(key); 
	}
	public double getPositiveCutoff() { return positiveCutoff; }
	public double getNegativeCutoff() { return negativeCutoff; }
	public Map<String, Object> getRINParameters() { return rinParameters; }

}
