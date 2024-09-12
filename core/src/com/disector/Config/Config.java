package com.disector.Config;

import com.badlogic.gdx.files.FileHandle;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Config {
    public int fov = 100;
    public boolean printFps = false;
    public boolean vsync = false;
    public boolean use32bitColor = false;
    public int frameWidth = 320;
    public int frameHeight = 180;
    public String palette = null;

    public Config(FileHandle file) {
        try {
            load(file);
        } catch (Exception e) {
            System.out.println("Error Loading Config");
        }
    }

    public Config() {

    }

    private void load(FileHandle file) {
        if (file == null)
            return;

        if (!file.exists() || file.isDirectory()) {
            System.out.println("File not Found when Loading Config");
            return;
        }

        System.out.println("Loading Config at " + file.path());
        Scanner fileScan = new Scanner(file.readString());

        Field[] fields = Config.class.getFields();
        Map<String, String> fileValues = new HashMap<>();

        while(fileScan.hasNextLine()) {
            String line = fileScan.nextLine();

            int equalsPosition = line.indexOf('=');
            if (equalsPosition == -1)  //Skip this line of the file is no '=' present
                continue;

            String varName = line.substring(0, equalsPosition);
            String varValue = line.substring(equalsPosition+1);

            boolean nameFound = false;
            for (Field field : fields) {
                if (varName.equals(field.getName())) {
                    nameFound = true;
                    break;
                }
            }

            if (nameFound)
                fileValues.put(varName, varValue);
            else
                System.out.println("    Invalid Name: " + varName);
        }

        String fieldName = "";
        String fieldType = "";
        String valueStr = "";

        try {
            for (Field field : fields) {
                fieldName = field.getName();
                fieldType = field.getType().getSimpleName();
                boolean fieldFound = false;

                valueStr = "";

                switch (fieldType) {
                    case "boolean":
                        valueStr = fileValues.get(fieldName);
                        if (valueStr != null) {
                            field.set(this, Boolean.parseBoolean(valueStr));
                            fieldFound = true;
                        }
                        break;
                    case "int":
                        valueStr = fileValues.get(fieldName);
                        if (valueStr != null) {
                            field.set(this, Integer.parseInt(valueStr));
                            fieldFound = true;
                        }
                        break;
                    case "String":
                        valueStr = fileValues.get(fieldName);
                        if (valueStr != null) {
                            field.set(this, valueStr);
                            fieldFound = true;
                        }
                }

                System.out.println("    " + fieldName + (!fieldFound ? "NOT FOUND.." : " set to " + field.get(this).toString() ));

            }
        } catch (IllegalAccessException | NumberFormatException e) {
            System.out.printf("    PARSING ERROR FOR %s %s WHEN GIVEN %s\n", fieldType.toUpperCase(), fieldName, valueStr );
        }
    }

}
