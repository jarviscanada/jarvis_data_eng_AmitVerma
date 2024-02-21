package ca.jrvs.apps.grep;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.BasicConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaGrepImp implements JavaGrep {

    static final Logger logger = LoggerFactory.getLogger(JavaGrep.class);
    private String regex;
    private String rootPath;
    private String outFile;

    public static void main(String[] args) {
        if (args.length != 3) {
            logger.error("Insufficient Command Line Arguments.");
            throw new IllegalArgumentException("Usage: JavaGrep [regex] [rootPath] [outputFile]");
        }
        BasicConfigurator.configure();

        JavaGrepImp javaGrepImp = new JavaGrepImp();

        javaGrepImp.setRegex(args[0]);
        javaGrepImp.setRootPath(args[1]);
        javaGrepImp.setOutFile(args[2]);

        try {
            javaGrepImp.process();
        } catch (Exception e) {
            logger.error("Error: Unable to process", e);
        }
    }

    @Override
    public void process() throws IOException {
        //Check if rootPath is an actual path
        if(!Files.exists(Paths.get(getRootPath()))) {
            throw new IOException(String.format("Error: %s No such file or directory", getRootPath()));
        }
        //Get list of files at path
        List<File> files = listFiles(getRootPath(), new ArrayList<>());
        //Convert the files List to a stream
        List<String> matches = files.stream()
                //flatMap flattens everything into a one dimensional array
                //This flattens all the readLines streams into a single stream
                .flatMap(file -> {
                    try {
                        return readLines(file).stream();
                    } catch (IOException e) {
                        logger.error("Error: Unable to process", e);
                        throw new RuntimeException(e);
                    }
                })
                //Convert the stream into a List
                .collect(Collectors.toList());

        writeToFile(matches);
    }

    @Override
    public List<File> listFiles(String rootDir, List<File> files) throws IOException {
        //Use walk method to provide a stream of all files and directories at the given path
        try (Stream<Path> pathStream = Files.walk(Path.of(rootDir))) {
            return pathStream
                    //Filter out the directories in the path and only grab the file
                    .filter(Files::isRegularFile)
                    //Map the path of the file to the stream
                    .map(Path::toFile)
                    //Convert to a List
                    .collect(Collectors.toList());
        }
    }

    @Override
    public List<String> readLines(File inputFile) throws IOException {
        try (Stream<String> lines = Files.lines(inputFile.toPath())) {
            //Use containsPattern to filter the lines
            return lines.filter(this::containsPattern)
                    //Make list of filtered lines
                    .collect(Collectors.toList());
        }
    }

    @Override
    public void writeToFile(List<String> lines) throws IOException {
        try {
            //Create the output file
            FileWriter fileWriter = new FileWriter(getOutFile());
            //Write each line from the lines List into the file
            for (String line : lines) {
                fileWriter.write(line +'\n');
            }
            fileWriter.close();
        } catch (IOException e) {
            logger.error("Error: Unable to process", e);
            throw new IOException(e);
        }
    }

    @Override
    public String getRegex() {
        return regex;
    }

    @Override
    public void setRegex(String regex) {
        this.regex = regex;
    }

    @Override
    public String getRootPath() {
        return rootPath;
    }

    @Override
    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    @Override
    public String getOutFile() {
        return outFile;
    }

    @Override
    public void setOutFile(String outFile) {
        this.outFile = outFile;
    }

    @Override
    public boolean containsPattern(String line) {
        Pattern pattern = Pattern.compile(getRegex());
        Matcher match = pattern.matcher(line);
        return match.matches();
    }
}