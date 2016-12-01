/**
 *
 */

package com.aritu.eloraplm.config.restoperations;

import java.io.IOException;
import java.util.Map;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.runtime.api.Framework;

import com.aritu.eloraplm.config.api.EloraConfigManager;
import com.aritu.eloraplm.config.util.EloraConfigRow;
import com.aritu.eloraplm.config.util.EloraConfigTable;
import com.aritu.eloraplm.exceptions.EloraException;

/**
 *
 */
@Operation(id = SyncConfigurations.ID, category = Constants.CAT_DOCUMENT, label = "EloraPlmConnector - Sync configurations", description = "")
public class SyncConfigurations {

    public static final String ID = "Elora.PlmConnector.SyncConfigurations";

    protected EloraConfigManager configManager = Framework.getService(EloraConfigManager.class);

    @Param(name = "config_table", required = true)
    public String configTable;

    @Param(name = "hashCode", required = false)
    public int hashCode;

    @Param(name = "force", required = true)
    public boolean force;

    @OperationMethod
    public String run() throws JsonGenerationException, JsonMappingException,
            IOException, EloraException {
        // TODO: Hablar con Mikel para ver cómo devolver la respuesta
        // Si se desordena map se puede probar con linkedhashmap. Esto es por el
        // tema de hashCode, ya que puede variar dependiendo del orden
        // TODO: Sacar a EloraConfigHelper
        EloraConfigTable vocabTable = configManager.getConfigTable(configTable,
                null, null);
        Map<String, EloraConfigRow> vocabMap = vocabTable.getRows();
        // Map<String, Map<String, Object>> vocabMap =
        // configService.getConfigTable(configTable, null, null);

        if (!force && vocabMap.hashCode() == hashCode) {
            return String.valueOf(hashCode);
        } else {
            ObjectMapper mapper = new ObjectMapper();
            String jsonResult;
            jsonResult = mapper.writeValueAsString(vocabMap);
            return jsonResult;
        }
    }

}
