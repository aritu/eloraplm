package com.aritu.eloraplm.core.converter;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.convert.plugins.CommandLineBasedConverter;

public class Image2PdfConverter extends CommandLineBasedConverter {

    @Override
    protected BlobHolder buildResult(List<String> cmdOutput,
            CmdParameters cmdParams) {

        String outputPath = cmdParams.getParameter("outDirPath");
        File outputDir = new File(outputPath);
        File[] files = outputDir.listFiles();
        List<Blob> blobs = new ArrayList<>();

        for (File file : files) {
            try {
                Blob blob = Blobs.createBlob(file);
                blob.setFilename(file.getName());
                blobs.add(blob);
            } catch (IOException e) {
                throw new ConversionException("Cannot create Blob", e);
            }
        }
        return new SimpleCachableBlobHolder(blobs);
    }

    @Override
    protected Map<String, Blob> getCmdBlobParameters(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {

        Map<String, Blob> cmdBlobParams = new HashMap<>();
        cmdBlobParams.put("inputFilePath", blobHolder.getBlob());
        return cmdBlobParams;
    }

    @Override
    protected Map<String, String> getCmdStringParameters(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {

        Map<String, String> cmdStringParams = new HashMap<>();

        String baseDir = getTmpDirectory(parameters);
        Path tmpPath = new Path(baseDir).append(
                "image2pdf_" + System.currentTimeMillis());

        File outDir = new File(tmpPath.toString());
        boolean dirCreated = outDir.mkdir();
        if (!dirCreated) {
            throw new ConversionException(
                    "Unable to create tmp dir for transformer output");
        }

        cmdStringParams.put("outDirPath", outDir.getAbsolutePath());
        cmdStringParams.put("outputFilePath", outDir.getAbsolutePath()
                + System.getProperty("file.separator")
                + FilenameUtils.getBaseName(blobHolder.getBlob().getFilename())
                + ".pdf");
        return cmdStringParams;
    }

}
