/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.upload;

import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.server.StreamVariable;
import com.vaadin.ui.Upload.FinishedEvent;
import com.vaadin.ui.Upload.SucceededEvent;

/**
 * {@link StreamVariable} implementation to read and upload a file. One instance
 * per file stream is used.
 *
 * The handler manages the output to the user and at the same time ensures that
 * the upload does not exceed the configured max file size.
 *
 */
public class FileTransferHandlerStreamVariable extends AbstractFileTransferHandler implements StreamVariable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(FileTransferHandlerStreamVariable.class);

    private final long maxSize;
    private final long fileSize;
    private final String mimeType;
    private final FileUploadId fileUploadId;

    FileTransferHandlerStreamVariable(final String fileName, final long fileSize, final long maxSize,
            final String mimeType, final SoftwareModule selectedSw, final ArtifactManagement artifactManagement,
            final VaadinMessageSource i18n) {
        super(artifactManagement, i18n);
        this.fileSize = fileSize;
        this.maxSize = maxSize;
        this.mimeType = mimeType;
        this.fileUploadId = new FileUploadId(fileName, selectedSw);

        publishUploadStarted(fileUploadId);
    }

    @Override
    public void streamingStarted(final StreamingStartEvent event) {
        assertStateConsistency(fileUploadId, event.getFileName());

        if (isFileAlreadyContainedInSoftwareModul(fileUploadId, fileUploadId.getSoftwareModule())) {
            LOG.info("File {} already contained in Software Module {}", fileUploadId.getFilename(),
                    fileUploadId.getSoftwareModule());
            setDuplicateFile();
        }
    }

    @Override
    public final OutputStream getOutputStream() {

        try {
            // we return the outputstream so we cannot close it here
            @SuppressWarnings("squid:S2095")
            final OutputStream out = createOutputStreamForTempFile();

            publishUploadProgressEvent(fileUploadId, 0, fileSize, getTempFilePath());

            return out;
        } catch (final IOException e) {
            LOG.error("Upload failed {}", e);
            setFailureReasonUploadFailed();
        }

        setUploadInterrupted();
        return new NullOutputStream();
    }

    /**
     * listen progress.
     * 
     * @return boolean
     */
    @Override
    public boolean listenProgress() {
        return true;
    }

    /**
     * Reports progress in {@link StreamVariable} variant. Interrupts
     *
     * @see com.vaadin.server.StreamVariable#onProgress(com.vaadin.server.StreamVariable.StreamingProgressEvent)
     */
    @Override
    public void onProgress(final StreamingProgressEvent event) {
        assertStateConsistency(fileUploadId, event.getFileName());

        if (event.getBytesReceived() > maxSize || event.getContentLength() > maxSize) {
            LOG.error("User tried to upload more than was allowed ({}).", maxSize);
            setFailureReasonFileSizeExceeded(maxSize);
            setUploadInterrupted();
            return;
        }
        if (isUploadInterrupted()) {
            return;
        }

        publishUploadProgressEvent(fileUploadId, event.getBytesReceived(), event.getContentLength(),
                getTempFilePath());
    }

    /**
     * Upload finished for {@link StreamVariable} variant. Called only in good
     * case. So a combination of {@link #uploadSucceeded(SucceededEvent)} and
     * {@link #uploadFinished(FinishedEvent)}.
     *
     * @see com.vaadin.server.StreamVariable#streamingFinished(com.vaadin.server.StreamVariable.StreamingEndEvent)
     */
    @Override
    public void streamingFinished(final StreamingEndEvent event) {
        assertStateConsistency(fileUploadId, event.getFileName());

        transferArtifactToRepository(fileUploadId, event.getContentLength(), mimeType, getTempFilePath());
    }

    /**
     * Upload failed for{@link StreamVariable} variant.
     *
     * @param event
     *            StreamingEndEvent
     */
    @Override
    public void streamingFailed(final StreamingErrorEvent event) {
        assertStateConsistency(fileUploadId, event.getFileName());

        if (isDuplicateFile()) {
            publishUploadFailedEvent(fileUploadId, getI18n().getMessage("message.no.duplicateFiles"),
                    event.getException());
        } else {
            publishUploadFailedEvent(fileUploadId, event.getException());
        }
        publishUploadFinishedEvent(fileUploadId);
    }

    @Override
    public boolean isInterrupted() {
        return isUploadInterrupted();
    }
}
