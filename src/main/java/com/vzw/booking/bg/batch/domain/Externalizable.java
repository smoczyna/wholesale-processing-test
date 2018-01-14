package com.vzw.booking.bg.batch.domain;

import com.vzw.booking.bg.batch.constants.ExternalizationFormat;
import com.vzw.booking.bg.batch.domain.ExternalizationMetadata.FieldMetaData;
import com.vzw.booking.bg.batch.domain.exceptions.ContentTooLongException;
import com.vzw.booking.bg.batch.domain.exceptions.ExternalizationException;
import com.vzw.booking.bg.batch.utils.ReflectionsUtility;
import java.lang.reflect.InvocationTargetException;

public interface Externalizable {

    /**
     * Produce appropriate output, according to fields and required output
     * format.
     *
     * @param metadata MetaData used for parsing output
     * @return output value for externalization
     * @throws com.vzw.booking.bg.batch.domain.exceptions.ExternalizationException
     */
    default String dump(ExternalizationMetadata metadata) throws ExternalizationException {
        String line = "";
        String currentField = "[none]";
        try {
            for (FieldMetaData meta : metadata.getMetaData()) {
                currentField = meta.getFieldName();
                if (metadata.getFormat() == ExternalizationFormat.COMMA_SEPARATED_FORMAT) {
                    line += (line.length() == 0 ? "" : ",") + ReflectionsUtility.objectToString(meta.getGetterMethod().invoke(this));
                } else {
                    line += ReflectionsUtility.objectToString(meta.getGetterMethod().invoke(this), meta.getOutputType(), '0', ' ', meta.getLength());
                }
            }
        } catch (ContentTooLongException | ExternalizationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            String outLine = "";
            try {
                outLine = asStringDescriptor(metadata.clone(ExternalizationFormat.COMMA_SEPARATED_FORMAT));
            } catch (ExternalizationException e1) {
                throw new ExternalizationException("Error merging line <field: " + currentField + "> : <NOT PARSABLE> => " + e1.getMessage());
            }
            throw new ExternalizationException("Error merging line <field: " + currentField + "> : " + outLine + " => " + e.getMessage());
        }
        return line;
    }

    default String asStringDescriptor(ExternalizationMetadata metadata) throws ExternalizationException {
        String line = "";
        try {
            for (FieldMetaData meta : metadata.getMetaData()) {
                if (metadata.getFormat() == ExternalizationFormat.COMMA_SEPARATED_FORMAT) {
                    line += (line.length() == 0 ? "" : "|") + ReflectionsUtility.objectToString(meta.getGetterMethod().invoke(this));
                } else {
                    line += (line.length() == 0 ? "" : "|") + ReflectionsUtility.objectToString(meta.getGetterMethod().invoke(this)) + "#" + meta.getLength();
                }
            }
        } catch (ExternalizationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new ExternalizationException("Error collating line => " + e.getMessage());
        }
        return line;
    }
}
