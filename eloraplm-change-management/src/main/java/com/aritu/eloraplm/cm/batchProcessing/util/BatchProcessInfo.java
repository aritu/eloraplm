/*
 * (C) Copyright 2015 Aritu S Coop (http://aritu.com/).
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package com.aritu.eloraplm.cm.batchProcessing.util;

/**
 * Batch process progress counter.
 *
 * @author aritu
 *
 */
public class BatchProcessInfo {

    // itemType refers to BOM (Item) or DOC (Document)
    public String itemType;

    // itemClass refers to MODIFIED item or IMPACTED item
    public String itemClass;

    public String processingAction;

    public int totalCount;

    public int processedCount;

    public boolean isInProgress = false;

    public int needToBeRefreshed;

    /**
     * @param totalCount
     * @param processedCount
     */
    public BatchProcessInfo(String itemType, String itemClass,
            String processingAction, int totalCount, int processedCount,
            boolean isInProgress, int needToBeRefreshed) {
        super();
        this.itemType = itemType;
        this.itemClass = itemClass;
        this.processingAction = processingAction;
        this.totalCount = totalCount;
        this.processedCount = processedCount;
        this.isInProgress = isInProgress;
        this.needToBeRefreshed = needToBeRefreshed;
    }

    /**
     * @return the itemType
     */
    public String getItemType() {
        return itemType;
    }

    /**
     * @param itemType the itemType to set
     */
    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    /**
     * @return the itemClass
     */
    public String getItemClass() {
        return itemClass;
    }

    /**
     * @param itemClass the itemClass to set
     */
    public void setItemClass(String itemClass) {
        this.itemClass = itemClass;
    }

    /**
     * @return the processingAction
     */
    public String getProcessingAction() {
        return processingAction;
    }

    /**
     * @param processingAction the processingAction to set
     */
    public void setProcessingAction(String processingAction) {
        this.processingAction = processingAction;
    }

    /**
     * @return the totalCount
     */
    public int getTotalCount() {
        return totalCount;
    }

    /**
     * @param totalCount the totalCount to set
     */
    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    /**
     * @return the processedCount
     */
    public int getProcessedCount() {
        return processedCount;
    }

    /**
     * @param processedCount the processedCount to set
     */
    public void setProcessedCount(int processedCount) {
        this.processedCount = processedCount;
    }

    /**
     * @return the isInProgress
     */
    public boolean getIsInProgress() {
        return isInProgress;
    }

    /**
     * @param isInProgress the isInProgress to set
     */
    public void setIsInProgress(boolean isInProgress) {
        this.isInProgress = isInProgress;
    }

    /**
     * @return the needToBeRefreshed
     */
    public int getNeedToBeRefreshed() {
        return needToBeRefreshed;
    }

    /**
     * @param needToBeRefreshed the needToBeRefreshed to set
     */
    public void setNeedToBeRefreshed(int needToBeRefreshed) {
        this.needToBeRefreshed = needToBeRefreshed;
    }

    public void increaseProcessCounter() {
        processedCount++;
    }

}
