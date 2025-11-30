package com.example.user_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

@Service
public class PythonScriptService {

    @Value("${python.script.path}")
    private String scriptPath;
    
    @Value("${python.executable:python3}")
    private String pythonExecutable;

    public String executeScript(String scriptName, String inputFile, String outputFile) throws Exception {
        String scriptFullPath = scriptPath + File.separator + scriptName;
        
        ProcessBuilder processBuilder = new ProcessBuilder(
            pythonExecutable, scriptFullPath, "--input", inputFile, "--csv-output", outputFile
        );
        
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Python script failed: " + output.toString());
        }
        
        return output.toString();
    }
}
