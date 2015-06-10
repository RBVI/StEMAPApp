package edu.ucsf.rbvi.stEMAP.internal.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;


import java.util.HashMap;
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

	public StructureMap(File stFile) throws IOException, FileNotFoundException {
		pdbId = null;
		chimeraCommands = "";
		chainMap = new HashMap<>();

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
				chainMap.put((String)obj, (String)chains.get(obj));
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
	public String getChain(String key) { return chainMap.get(key); }

}
