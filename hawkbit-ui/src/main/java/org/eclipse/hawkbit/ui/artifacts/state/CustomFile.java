/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.state;

import java.io.Serializable;

import org.eclipse.hawkbit.repository.model.SoftwareModule;

/**
 * Custom file to hold details of uploaded file.
 *
 *
 *
 */
public class CustomFile implements Serializable {

    private static final long serialVersionUID = -5902321650745311767L;

    private final String fileName;

    private long fileSize;

    private String filePath;

    private final String baseSoftwareModuleName;

    private final String baseSoftwareModuleVersion;

    private String mimeType;

    /**
     * Used to specify if the file is uploaded successfully.
     */
    private final Boolean isValid = Boolean.TRUE;

    /**
     * Reason if upload fails.
     */
    private String failureReason;

    private SoftwareModule softwareModule;

    /**
     * Initialize details.
     *
     * @param fileName
     *            uploaded file name
     * @param fileSize
     *            uploaded file size
     * @param filePath
     *            uploaded file path
     * @param baseSoftwareModuleName
     *            software module name
     * @param baseSoftwareModuleVersion
     *            software module version
     * @param mimeType
     *            the mimeType of the file
     */
    public CustomFile(final String fileName, final long fileSize, final String filePath,
            final String baseSoftwareModuleName, final String baseSoftwareModuleVersion, final String mimeType,
            final SoftwareModule softwareModule) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.filePath = filePath;
        this.baseSoftwareModuleName = baseSoftwareModuleName;
        this.baseSoftwareModuleVersion = baseSoftwareModuleVersion;
        this.mimeType = mimeType;
        this.softwareModule = softwareModule;
    }

    /**
     * Initialize details.
     *
     * @param fileName
     *            uploaded file name
     * @param baseSoftwareModuleName
     *            software module name
     * @param baseSoftwareModuleVersion
     *            software module version
     */
    public CustomFile(final String fileName, final String baseSoftwareModuleName,
            final String baseSoftwareModuleVersion) {
        this.fileName = fileName;
        this.baseSoftwareModuleName = baseSoftwareModuleName;
        this.baseSoftwareModuleVersion = baseSoftwareModuleVersion;
    }

    public String getBaseSoftwareModuleName() {
        return baseSoftwareModuleName;
    }

    public String getBaseSoftwareModuleVersion() {
        return baseSoftwareModuleVersion;
    }


    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(final long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Boolean getIsValid() {
        return isValid;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public SoftwareModule getSoftwareModule() {
        return softwareModule;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((baseSoftwareModuleName == null) ? 0 : baseSoftwareModuleName.hashCode());
        result = prime * result + ((baseSoftwareModuleVersion == null) ? 0 : baseSoftwareModuleVersion.hashCode());
        result = prime * result + ((fileName == null) ? 0 : fileName.hashCode());
        result = prime * result + ((softwareModule == null) ? 0 : softwareModule.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CustomFile other = (CustomFile) obj;
        if (baseSoftwareModuleName == null) {
            if (other.baseSoftwareModuleName != null) {
                return false;
            }
        } else if (!baseSoftwareModuleName.equals(other.baseSoftwareModuleName)) {
            return false;
        }
        if (baseSoftwareModuleVersion == null) {
            if (other.baseSoftwareModuleVersion != null) {
                return false;
            }
        } else if (!baseSoftwareModuleVersion.equals(other.baseSoftwareModuleVersion)) {
            return false;
        }
        if (softwareModule == null) {
            if (other.softwareModule != null) {
                return false;
            }
        } else if (!softwareModule.equals(other.softwareModule)) {
            return false;
        }
        if (fileName == null) {
            if (other.fileName != null) {
                return false;
            }
        } else if (!fileName.equals(other.fileName)) {
            return false;
        }
        return true;
    }

}
