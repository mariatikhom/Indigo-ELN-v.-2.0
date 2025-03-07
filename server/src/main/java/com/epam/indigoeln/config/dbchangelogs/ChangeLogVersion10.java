/*
 *  Copyright (C) 2015-2018 EPAM Systems
 *  
 *  This file is part of Indigo ELN.
 *
 *  Indigo ELN is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Indigo ELN is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Indigo ELN.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.epam.indigoeln.config.dbchangelogs;

import com.epam.indigoeln.core.model.User;
import com.mongodb.DBRef;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;

import java.util.*;

@ChangeUnit(id = "init10", order = "001")
@RequiredArgsConstructor
public final class ChangeLogVersion10 {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeLogVersion10.class);

    private static final String UNIQUE_KEY = "unique";
    private static final String PROJECT_COLLECTION_NAME = "project";
    private static final String NOTEBOOK_COLLECTION_NAME = "notebook";
    private static final String EXPERIMENT_COLLECTION_NAME = "experiment";
    private static final String ADMIN = "admin";
    private static final String SYSTEM = "system";
    private static final String SEQUENCE_ID = "sequenceId";
    private static final String ID_KEY = "_id";
    private static final String ROLE_ID = "role-0";
    private static final String THERAPEUTIC_AREA_ID = "therapeuticArea";
    private static final String PROJECT_CODE_ID = "projectCode";
    private static final String SOURCE_ID = "source";
    private static final String SOURCE_DETAIL_ID = "sourceDetail";
    private static final String STEREOISOMER_CODE_ID = "stereoisomerCode";
    private static final String COMPOUND_STATE_ID = "compoundState";
    private static final String COMPOUND_PROTECTION_ID = "compoundProtection";
    private static final String SOLVENT_NAME_ID = "solventName";
    private static final String PURITY_ID = "purity";
    private static final String HEALTH_HAZARDS_ID = "healthHazards";
    private static final String HANDLING_PRECAUTIONS_ID = "handlingPrecautions";
    private static final String STORAGE_INSTRUCTIONS = "storageInstructions";

    private final MongoDatabase db;
    private final Environment environment;

    @Execution
    public void changeSet() {
        initIndexes();
        initRoles();
        initUsers();
        initDataDictionaries();
    }

    @RollbackExecution
    public void rollback() {
        throw new UnsupportedOperationException("Rollback not supported");
    }

    void initIndexes() {
        db.getCollection("role").createIndex(new Document("name", 1),
                new IndexOptions().unique(true));

        db.getCollection("user").createIndex(new Document("login", 1),
                new IndexOptions().unique(true));
        db.getCollection("user").createIndex(new Document("email", 1));
        db.getCollection("user").createIndex(new Document("roles", 1));

        db.getCollection(PROJECT_COLLECTION_NAME).createIndex(new Document("name", 1),
                new IndexOptions().unique(true));
        db.getCollection(PROJECT_COLLECTION_NAME).createIndex(new Document("accessList.user", 1));
        db.getCollection(PROJECT_COLLECTION_NAME).createIndex(new Document("notebooks", 1));
        db.getCollection(PROJECT_COLLECTION_NAME).createIndex(new Document("fileIds", 1));
        db.getCollection(PROJECT_COLLECTION_NAME).createIndex(new Document(SEQUENCE_ID, 1));

        db.getCollection(NOTEBOOK_COLLECTION_NAME).createIndex(new Document("name", 1), new IndexOptions().unique(true));
        db.getCollection(NOTEBOOK_COLLECTION_NAME).createIndex(new Document("experiments", 1));
        db.getCollection(NOTEBOOK_COLLECTION_NAME).createIndex(new Document(SEQUENCE_ID, 1));

        db.getCollection(EXPERIMENT_COLLECTION_NAME).createIndex(new Document("fileIds", 1));
        db.getCollection(EXPERIMENT_COLLECTION_NAME).createIndex(new Document(SEQUENCE_ID, 1));
        db.getCollection(EXPERIMENT_COLLECTION_NAME).createIndex(new Document("experimentFullName", 1));

        db.getCollection("component").createIndex(new Document(EXPERIMENT_COLLECTION_NAME, 1));
    }

    void initRoles() {
        final MongoCollection<Document> collection = db.getCollection("role");
        collection.insertOne(DocumentBuilder.of()
                .add(ID_KEY, objectId(ROLE_ID))
                .add("name", "All Permissions")
                .add(SYSTEM, true)
                .add("authorities", Arrays.asList(
                        "USER_EDITOR", "ROLE_EDITOR", "CONTENT_EDITOR",
                        "PROJECT_READER", "NOTEBOOK_READER", "EXPERIMENT_READER",
                        "PROJECT_CREATOR", "NOTEBOOK_CREATOR", "EXPERIMENT_CREATOR",
                        "PROJECT_REMOVER", "NOTEBOOK_REMOVER", "EXPERIMENT_REMOVER",
                        "TEMPLATE_EDITOR", "DICTIONARY_EDITOR", "GLOBAL_SEARCH"
                )).build());
    }

    void initUsers() {
        final MongoCollection<Document> collection = db.getCollection("user");
        collection.insertOne(DocumentBuilder.of()
                .add(ID_KEY, ADMIN)
                .add("login", ADMIN)
                .add("password", environment.getProperty("default-admin-password"))
                .add("first_name", ADMIN)
                .add("last_name", "Administrator")
                .add("email", "admin@localhost")
                .add("activated", true)
                .add(SYSTEM, true)
                .add("lang_key", "en")
                .add("created_by", SYSTEM)
                .add("created_date", new Date())
                .add("roles", Collections.singletonList(new DBRef("role", objectId(ROLE_ID))))
                .build());
    }

    void initDataDictionaries() {

        List<Document> therapeuticAreaList = Arrays.asList(
                createDictionaryWord("Obesity", null, true, 0),
                createDictionaryWord("Diabet", null, true, 1),
                createDictionaryWord("Pulmonology", null, true, 2),
                createDictionaryWord("Cancer", null, true, 3)
        );
        createDictionary(THERAPEUTIC_AREA_ID, "Therapeutic Area", "Therapeutic Area",
                therapeuticAreaList, db);

        final List<Document> projectCodeList = Arrays.asList(
                createDictionaryWord("Code 1", null, true, 0),
                createDictionaryWord("Code 2", null, true, 1),
                createDictionaryWord("Code 3", null, true, 2)
        );

        createDictionary(PROJECT_CODE_ID, "Project Code & Name", "Project Code for "
                + "experiment details", projectCodeList, db);

        final List<Document> sourceList = Arrays.asList(
                createDictionaryWord("Source1", null, true, 0),
                createDictionaryWord("Source2", null, true, 1)
        );

        createDictionary(SOURCE_ID, "Source", "Source", sourceList, db);

        final List<Document> sourceDetailsList = Arrays.asList(
                createDictionaryWord("Source Details1", null, true, 0),
                createDictionaryWord("Source Details2", null, true, 1),
                createDictionaryWord("Source Details3", null, true, 2)
        );

        createDictionary(SOURCE_DETAIL_ID,
                "Source Details", "Source Details", sourceDetailsList, db);

        final List<Document> stereoisomerCodeList = Arrays.asList(
                createDictionaryWord("NOSTC", "Achiral - No Stereo Centers",
                        true, 0),
                createDictionaryWord("AMESO", "Achiral - Meso Stereomers",
                        true, 1),
                createDictionaryWord("CISTR", "Achiral - Cis/Trans Stereomers",
                        true, 2),
                createDictionaryWord("SNENK", "Single Enantiomer (chirality known)",
                        true, 3),
                createDictionaryWord("RMCMX", "Racemic (stereochemistry known)",
                        true, 4),
                createDictionaryWord("ENENK", "Enantio-Enriched (chirality known)",
                        true, 5),
                createDictionaryWord("DSTRK", "Diastereomers (stereochemistry known)",
                        true, 6),
                createDictionaryWord("SNENU",
                        "Other - Single Enantiomer (chirality unknown)",
                        true, 7),
                createDictionaryWord("LRCMX", "Other - Racemic (relative "
                        + "stereochemistry unknown)", true, 8),
                createDictionaryWord("ENENU",
                        "Other - Enantio-Enriched (chirality unknown)",
                        true, 9),
                createDictionaryWord("DSTRU", "Other - Diastereomers "
                        + "(relative stereochemistry unknown)", true, 10),
                createDictionaryWord("UNKWN", "Other - Unknown Stereomer/Mixture",
                        true, 11),
                createDictionaryWord("HSREG", "Flag for automatic stereoisomer "
                        + "code assignment for multi-registration", true, 12),
                createDictionaryWord("ACHIR", "ACHIRAL", true, 13),
                createDictionaryWord("HOMO", "HOMO-CHIRAL", true, 14),
                createDictionaryWord("MESO", "MESO", true, 15),
                createDictionaryWord("RACEM", "RACEMIC", true, 16),
                createDictionaryWord("SCALE", "SCALEMIC", true, 17)
        );

        createDictionary(STEREOISOMER_CODE_ID, "Stereoisomer Code",
                "Stereoisomer Code", stereoisomerCodeList, db);

        final List<Document> compoundStateList = Arrays.asList(
                createDictionaryWord("Solid", null, true, 0),
                createDictionaryWord("Gas", null, true, 1),
                createDictionaryWord("oil", null, true, 2),
                createDictionaryWord("liquid", null, true, 3)
        );

        createDictionary(COMPOUND_STATE_ID,
                "Compound State", "Compound State", compoundStateList, db);

        final List<Document> compoundProtectionList = Arrays.asList(
                createDictionaryWord("Compound Protection1", null, true, 0),
                createDictionaryWord("Compound Protection2", null, true, 1),
                createDictionaryWord("Compound Protection3", null, true, 2)
        );

        createDictionary(COMPOUND_PROTECTION_ID, "Compound Protection",
                "Compound Protection", compoundProtectionList, db);

        final List<Document> solventNameList = Arrays.asList(
                createDictionaryWord("Acetic acid", null, true, 0),
                createDictionaryWord("Hydrochloric acid", null, true, 1),
                createDictionaryWord("Fumaric acid", null, true, 2),
                createDictionaryWord("Formaldehyde", null, true, 3),
                createDictionaryWord("Sulfuric acid", null, true, 4)
        );

        createDictionary(SOLVENT_NAME_ID, "Solvent Name", "Solvent Name", solventNameList, db);

        final List<Document> purityList = Arrays.asList(
                createDictionaryWord("NMR", null, true, 0),
                createDictionaryWord("HPLC", null, true, 1),
                createDictionaryWord("LCMS", null, true, 2),
                createDictionaryWord("CHN", null, true, 3),
                createDictionaryWord("MS", null, true, 4)
        );

        createDictionary(PURITY_ID, "Purity", "Purity definition methods", purityList, db);

        final List<Document> healthHazardsList = Arrays.asList(
                createDictionaryWord("Very Toxic", null, true, 0),
                createDictionaryWord("Explosive, Potential", null, true, 1),
                createDictionaryWord("Carcinogen", null, true, 2),
                createDictionaryWord("Corrosive - Acid", null, true, 3),
                createDictionaryWord("Mutagen", null, true, 4),
                createDictionaryWord("Flammable", null, true, 5)
        );

        createDictionary(HEALTH_HAZARDS_ID, "Health Hazards", "Health Hazards",
                healthHazardsList, db);

        final List<Document> handlingPrecautionsList = Arrays.asList(
                createDictionaryWord("Electrostatic", null, true, 0),
                createDictionaryWord("Hygroscopic", null, true, 1),
                createDictionaryWord("Oxidiser", null, true, 2),
                createDictionaryWord("Air Sensitive", null, true, 3),
                createDictionaryWord("Moisture Sensitive", null, true, 4)
        );

        createDictionary(HANDLING_PRECAUTIONS_ID, "Handling Precautions",
                "Handling Precautions", handlingPrecautionsList, db);

        final List<Document> storageInstructionsList = Arrays.asList(
                createDictionaryWord("No Special Storage Required", null,
                        true, 0),
                createDictionaryWord("Store in Refrigerator", null, true, 1),
                createDictionaryWord("Store Under Argon", null, true, 2),
                createDictionaryWord("Keep tightly sealed", null, true, 3)
        );

        createDictionary(STORAGE_INSTRUCTIONS, "Storage Instructions",
                "Storage Instructions", storageInstructionsList, db);

    }

    private void createDictionary(String id, String name, String description, List<Document> words, MongoDatabase db) {
        MongoCollection<Document> dictionary = db.getCollection("dictionary");
        dictionary.insertOne(DocumentBuilder.of()
                .add(ID_KEY, objectId(id))
                .add("name", name)
                .add("description", description)
                .add("words", words)
                .add("accessList", Collections.emptyList())
                .add("author", new DBRef(User.COLLECTION_NAME, ADMIN))
                .build());
    }

    private Document createDictionaryWord(String name, String description, boolean enable, int rank) {
        return DocumentBuilder.of()
                .add(ID_KEY, null)
                .add("name", name)
                .add("description", description)
                .add("enable", enable)
                .add("rank", rank)
                .add("accessList", Collections.emptyList())
                .build();
    }

    private static ObjectId objectId(String id) {
        return new ObjectId(DigestUtils.md5Hex(id).substring(0, 24));
    }

    private static class DocumentBuilder {

        private final Map<String, Object> map = new LinkedHashMap<>();

        static DocumentBuilder of() {
            return new DocumentBuilder();
        }

        static DocumentBuilder of(String key, Object value) {
            return new DocumentBuilder().add(key, value);
        }

        DocumentBuilder add(String key, Object value) {
            map.put(key, value);
            return this;
        }

        Document build() {
            return new Document(map);
        }
    }
}
