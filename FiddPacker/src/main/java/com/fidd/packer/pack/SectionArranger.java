package com.fidd.packer.pack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.random.RandomGenerator;

import static com.fidd.packer.pack.FiddPackManager.randomLongBetween;

public class SectionArranger {
    public static List<SectionDescriptor> rearrangeSections(List<SectionDescriptor> sectionList,
                                                            long minGapSize,
                                                            long maxGapSize,
                                                            RandomGenerator randomGenerator,
                                                            boolean alignAllMetadatas
                                                            ) {
        if (!alignAllMetadatas) {
            FiddPackManager.shuffle(sectionList, randomGenerator);
            for (SectionDescriptor sectionDescriptor : sectionList) {
                final long gapSize = randomLongBetween(minGapSize, maxGapSize, randomGenerator);
                sectionDescriptor.setGapBefore(gapSize);
            }
            return sectionList;
        } else {
            List<SectionDescriptor> fileList = new ArrayList<>();
            List<SectionDescriptor> metadataList = new ArrayList<>();
            for (SectionDescriptor sectionDescriptor : sectionList) {
                if (sectionDescriptor instanceof LogicalFileSectionDescriptor) {
                    fileList.add(sectionDescriptor);
                } else if (sectionDescriptor instanceof LogicalFileMetadataHeaderSectionDescriptor
                        || sectionDescriptor instanceof FiddFileMetadataSectionDescriptor) {
                    metadataList.add(sectionDescriptor);
                } else {
                    throw new RuntimeException("Unknown SectionDescriptor type " + sectionDescriptor.getClass());
                }
            }

            FiddPackManager.shuffle(fileList, randomGenerator);
            FiddPackManager.shuffle(metadataList, randomGenerator);

            // Random position in the file for contiguous Metadata Sections block
            int metadataSectionPosition = randomGenerator.nextInt(fileList.size()+1);

            List<SectionDescriptor> returnList = new ArrayList<>();
            for (int i = 0; i < fileList.size(); i++) {
                if (metadataSectionPosition == i) {
                    addAlignedMetadataAtCurrentPosition(returnList, metadataList, minGapSize, maxGapSize, randomGenerator);
                }

                final long gapSize = randomLongBetween(minGapSize, maxGapSize, randomGenerator);
                SectionDescriptor fileDescriptor = fileList.get(i);
                fileDescriptor.setGapBefore(gapSize);
                returnList.add(fileDescriptor);
            }

            if (metadataSectionPosition == fileList.size()) {
                addAlignedMetadataAtCurrentPosition(returnList, metadataList, minGapSize, maxGapSize, randomGenerator);
            }

            return returnList;
        }
    }

    static void addAlignedMetadataAtCurrentPosition(List<SectionDescriptor> returnList,
                                             List<SectionDescriptor> metadataList,
                                             long minGapSize,
                                             long maxGapSize,
                                             RandomGenerator randomGenerator) {
        for (int i = 0; i < metadataList.size(); i++) {
            SectionDescriptor metadataSectionDescriptor = metadataList.get(i);
            if (i == 0) {
                final long gapSize = randomLongBetween(minGapSize, maxGapSize, randomGenerator);
                metadataSectionDescriptor.setGapBefore(gapSize);
            } else {
                metadataSectionDescriptor.setGapBefore(0);
            }
            returnList.add(metadataSectionDescriptor);
        }
    }
}
