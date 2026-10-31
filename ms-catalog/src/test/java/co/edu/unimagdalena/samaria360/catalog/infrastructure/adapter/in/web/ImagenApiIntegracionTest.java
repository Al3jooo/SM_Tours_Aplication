package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.catalog.infrastructure.PruebaIntegracion;
import com.jayway.jsonpath.JsonPath;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import org.bson.BsonObjectId;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** {@code GET /api/v1/catalogo/imagenes/{id}} (DEC-209, ADR-009): pública, con el tipo del formato y en caché. */
@PruebaIntegracion
class ImagenApiIntegracionTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MongoTemplate mongo;

    @Test
    void sirveLaImagenConSuTipoYCacheInmutableSinToken() throws Exception {
        byte[] png = SolicitudesAtractivo.imagen(SolicitudesAtractivo.FIRMA_PNG, 300);
        ObjectId id = new ObjectId();
        GridFSBuckets.create(mongo.getDb(), "imagenes").uploadFromStream(new BsonObjectId(id), "a.png",
                new ByteArrayInputStream(png), new GridFSUploadOptions()
                        .metadata(new Document("formato", "PNG").append("tipoContenido", "image/png")));
        try {
            MvcResult resultado = mvc.perform(get("/api/v1/catalogo/imagenes/" + id.toHexString())).andReturn();

            assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
            assertThat(resultado.getResponse().getContentType()).isEqualTo("image/png");
            assertThat(resultado.getResponse().getContentAsByteArray()).isEqualTo(png);
            assertThat(resultado.getResponse().getHeader(HttpHeaders.CACHE_CONTROL))
                    .contains("max-age=31536000", "public", "immutable");
            assertThat(resultado.getResponse().getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        } finally {
            GridFSBuckets.create(mongo.getDb(), "imagenes").delete(id);
        }
    }

    @Test
    void unIdInvalidoInexistenteOUnArchivoAjenoEs404ImagenNoEncontrada() throws Exception {
        ObjectId ajeno = new ObjectId();
        GridFSBuckets.create(mongo.getDb(), "imagenes").uploadFromStream(new BsonObjectId(ajeno), "x.bin",
                new ByteArrayInputStream(new byte[]{1, 2, 3}));
        try {
            for (String id : List.of("abc", new ObjectId().toHexString(), ajeno.toHexString())) {
                MvcResult resultado = mvc.perform(get("/api/v1/catalogo/imagenes/" + id)).andReturn();
                assertThat(resultado.getResponse().getStatus()).as(id).isEqualTo(404);
                assertThat(JsonPath.<String>read(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8),
                        "$.codigo")).isEqualTo("IMAGEN_NO_ENCONTRADA");
            }
        } finally {
            GridFSBuckets.create(mongo.getDb(), "imagenes").delete(ajeno);
        }
    }
}
