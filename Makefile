JAVAC=javac
JAVA=java
JAR=jar

ROOT_DIR=$(shell pwd)
LIB_DIR=$(ROOT_DIR)/libs
SRC_DIR=$(ROOT_DIR)/src
OUT_DIR=$(ROOT_DIR)/out
MANIFEST_DIR=$(ROOT_DIR)/META-INF
BUILD_DIR=$(ROOT_DIR)/build
CFG_DIR=$(ROOT_DIR)/cfg

SRCS=$(shell find $(SRC_DIR) -name '*.java')

BUILD_JAR_NAME=ARA-Manet.jar

.PHONY: all clean

all: classes jar

out_dir:
	mkdir -p $(OUT_DIR)

build_dir:
	mkdir -p $(BUILD_DIR)

extract-libs-jar: out_dir
	cp $(LIB_DIR)/*.jar $(OUT_DIR)
	(cd $(OUT_DIR) && find -name '*.jar' -exec $(JAR) xf {} \;)
	rm $(OUT_DIR)/*.jar

classes: out_dir extract-libs-jar
	(cd $(SRC_DIR) && $(JAVAC) -cp $(OUT_DIR) -d $(OUT_DIR) $(SRCS))

move-manifest: out_dir $(MANIFEST_DIR)
	cp -r $(MANIFEST_DIR) $(OUT_DIR)

jar: classes move-manifest build_dir
	(cd $(OUT_DIR) && $(JAR) cmvf META-INF/MANIFEST.MF $(BUILD_DIR)/$(BUILD_JAR_NAME) .)

clean:
	rm -rf $(OUT_DIR)
	rm -rf $(BUILD_DIR)

ex1q10 :
	@make && java -jar build/$(BUILD_JAR_NAME) $(CFG_DIR)/$@.cfg

ex2q1 :
	@make && java -jar build/$(BUILD_JAR_NAME) $(CFG_DIR)/$@.cfg

ex2q4 :
	@make && java -jar build/$(BUILD_JAR_NAME) $(CFG_DIR)/$@.cfg

ex2q5 :
	@make && java -jar build/$(BUILD_JAR_NAME) $(CFG_DIR)/$@.cfg

ex2q6 :
	@make && java -jar build/$(BUILD_JAR_NAME) $(CFG_DIR)/$@.cfg

ex2q7 :
	@make && java -jar build/$(BUILD_JAR_NAME) $(CFG_DIR)/$@.cfg

ex2q8_1 :
	@make && java -jar build/$(BUILD_JAR_NAME) $(CFG_DIR)/$@.cfg

ex2q8_2 :
	@make && java -jar build/$(BUILD_JAR_NAME) $(CFG_DIR)/$@.cfg