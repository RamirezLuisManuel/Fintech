package com.proyecto.servicios.controller;

import com.proyecto.servicios.entity.*;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.repositorys.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/catalogos")
public class CatalogoController {

    @Autowired
    private CatPaisRepository catPaisRepository;

    @Autowired
    private CatOcupacionRepository catOcupacionRepository;

    @Autowired
    private CatEstadoRepository catEstadoRepository;

    @Autowired
    private CatMunicipioRepository catMunicipioRepository;

    @Autowired
    private CatColoniaRepository catColoniaRepository;

    private String normalize(String text) {
        if (text == null) return "";
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toLowerCase().trim();
    }

    private boolean matchesQuery(String nombre, String q) {
        if (q == null || q.isBlank()) return true;
        String normNombre = normalize(nombre);
        String[] tokens = normalize(q).split("\\s+");
        for (String token : tokens) {
            if (!token.isEmpty() && !normNombre.contains(token)) {
                return false;
            }
        }
        return true;
    }

    @GetMapping("/paises")
    public ResponseEntity<GenericResponse<List<Map<String, Object>>>> obtenerPaises(
            @RequestParam(required = false) String q) {
        List<CatPais> paises = catPaisRepository.findAllByOrderByIdAsc();
        List<Map<String, Object>> result = new ArrayList<>();
        Integer otroId = -1;
        for (CatPais p : paises) {
            if ("Otro".equalsIgnoreCase(p.getNombre())) {
                otroId = p.getId();
                continue;
            }
            if (matchesQuery(p.getNombre(), q)) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", p.getId());
                item.put("nombre", p.getNombre());
                result.add(item);
            }
        }
        Map<String, Object> otro = new LinkedHashMap<>();
        otro.put("id", otroId);
        otro.put("nombre", "Otro");
        result.add(otro);
        return ResponseEntity.ok(GenericResponse.exito("Catálogo de países obtenido exitosamente", result));
    }

    @GetMapping("/ocupaciones")
    public ResponseEntity<GenericResponse<List<Map<String, Object>>>> obtenerOcupaciones(
            @RequestParam(required = false) String q) {
        List<CatOcupacion> ocupaciones = catOcupacionRepository.findAllByOrderByIdAsc();
        List<Map<String, Object>> result = new ArrayList<>();
        Integer otraId = -1;
        for (CatOcupacion o : ocupaciones) {
            if ("Otra".equalsIgnoreCase(o.getNombre()) || "Otro".equalsIgnoreCase(o.getNombre())) {
                otraId = o.getId();
                continue;
            }
            if (matchesQuery(o.getNombre(), q)) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", o.getId());
                item.put("nombre", o.getNombre());
                result.add(item);
            }
        }
        Map<String, Object> otra = new LinkedHashMap<>();
        otra.put("id", otraId);
        otra.put("nombre", "Otra");
        result.add(otra);
        return ResponseEntity.ok(GenericResponse.exito("Catálogo de ocupaciones obtenido exitosamente", result));
    }

    @GetMapping("/estados")
    public ResponseEntity<GenericResponse<List<Map<String, Object>>>> obtenerEstados(
            @RequestParam Integer paisId,
            @RequestParam(required = false) String q) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (paisId != null && paisId > 0) {
            List<CatEstado> estados = catEstadoRepository.findByPaisIdOrderByNombreAsc(paisId);
            for (CatEstado e : estados) {
                if (matchesQuery(e.getNombre(), q)) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", e.getId());
                    item.put("nombre", e.getNombre());
                    result.add(item);
                }
            }
        }
        Map<String, Object> otro = new LinkedHashMap<>();
        otro.put("id", -1);
        otro.put("nombre", "Otro");
        result.add(otro);
        return ResponseEntity.ok(GenericResponse.exito("Catálogo de estados obtenido exitosamente", result));
    }

    @GetMapping("/municipios")
    public ResponseEntity<GenericResponse<List<Map<String, Object>>>> obtenerMunicipios(
            @RequestParam Integer estadoId,
            @RequestParam(required = false) String q) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (estadoId != null && estadoId > 0) {
            List<CatMunicipio> municipios = catMunicipioRepository.findByEstadoIdOrderByNombreAsc(estadoId);
            for (CatMunicipio m : municipios) {
                if (matchesQuery(m.getNombre(), q)) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", m.getId());
                    item.put("nombre", m.getNombre());
                    result.add(item);
                }
            }
        }
        Map<String, Object> otro = new LinkedHashMap<>();
        otro.put("id", -1);
        otro.put("nombre", "Otro");
        result.add(otro);
        return ResponseEntity.ok(GenericResponse.exito("Catálogo de municipios obtenido exitosamente", result));
    }

    @GetMapping("/colonias")
    public ResponseEntity<GenericResponse<List<Map<String, Object>>>> obtenerColonias(
            @RequestParam Integer municipioId,
            @RequestParam(required = false) String q) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (municipioId != null && municipioId > 0) {
            List<CatColonia> colonias = catColoniaRepository.findByMunicipioIdOrderByNombreAsc(municipioId);
            for (CatColonia c : colonias) {
                if (matchesQuery(c.getNombre(), q)) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", c.getId());
                    item.put("nombre", c.getNombre());
                    item.put("codigoPostal", c.getCodigoPostal());
                    result.add(item);
                }
            }
        }
        Map<String, Object> otra = new LinkedHashMap<>();
        otra.put("id", -1);
        otra.put("nombre", "Otra");
        otra.put("codigoPostal", null);
        result.add(otra);
        return ResponseEntity.ok(GenericResponse.exito("Catálogo de colonias obtenido exitosamente", result));
    }
}
