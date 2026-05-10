package org.example.util;

import javafx.application.Platform;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.example.event.Event;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Carte interactive Leaflet — version corrigée pour JavaFX WebView.
 *
 * Problème racine : loadContent() bloque les requêtes HTTPS vers les tuiles OSM.
 * Solution : écrire le HTML dans un fichier temporaire et charger via engine.load(file://)
 * → JavaFX autorise les requêtes réseau depuis file:// avec le bon User-Agent.
 */
public class StudentMapView extends VBox {

    private final WebView    webView;
    private final WebEngine  engine;
    private final MapService mapService = new MapService();
    private Consumer<Integer> onEventClick;
    private String  pendingJson = null;
    private boolean ready       = false;
    private File    tempHtmlFile = null;

    public StudentMapView() {
        webView = new WebView();
        webView.setPrefWidth(Double.MAX_VALUE);
        webView.setPrefHeight(Double.MAX_VALUE);
        webView.setMinHeight(420);
        webView.setContextMenuEnabled(false);
        VBox.setVgrow(webView, Priority.ALWAYS);
        getChildren().add(webView);
        VBox.setVgrow(this, Priority.ALWAYS);

        engine = webView.getEngine();
        engine.setUserAgent(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/124.0.0.0 Safari/537.36"
        );

        engine.getLoadWorker().stateProperty().addListener((obs, o, n) -> {
            if (n != javafx.concurrent.Worker.State.SUCCEEDED) return;
            ready = true;

            // Bridge Java ↔ JS
            try {
                JSObject w = (JSObject) engine.executeScript("window");
                w.setMember("javaApp", new JavaBridge());
            } catch (Exception ignored) {}

            // invalidateSize après chargement
            engine.executeScript(
                "setTimeout(function(){ if(typeof map!=='undefined') map.invalidateSize(true); }, 300);" +
                "setTimeout(function(){ if(typeof map!=='undefined') map.invalidateSize(true); }, 800);" +
                "setTimeout(function(){ if(typeof map!=='undefined') map.invalidateSize(true); }, 1800);"
            );

            if (pendingJson != null) {
                final String j = pendingJson;
                pendingJson = null;
                Platform.runLater(() -> {
                    try { engine.executeScript("loadMarkers(" + j + ")"); }
                    catch (Exception ignored) {}
                });
            }
        });

        webView.widthProperty().addListener((obs, o, n)  -> triggerInvalidate());
        webView.heightProperty().addListener((obs, o, n) -> triggerInvalidate());

        loadMap();
    }

    private void triggerInvalidate() {
        if (!ready) return;
        Platform.runLater(() -> {
            try { engine.executeScript("if(typeof map!=='undefined') map.invalidateSize(true);"); }
            catch (Exception ignored) {}
        });
    }

    public void setOnEventClick(Consumer<Integer> h) { this.onEventClick = h; }

    public void loadEvents(List<Event> events, Map<Integer, Integer> resCounts) {
        new Thread(() -> {
            String json = mapService.buildMarkersJson(events, resCounts);
            Platform.runLater(() -> {
                if (ready) {
                    try {
                        engine.executeScript("loadMarkers(" + json + ")");
                        engine.executeScript(
                            "setTimeout(function(){ if(typeof map!=='undefined') map.invalidateSize(true); }, 400);"
                        );
                    } catch (Exception e) {
                        System.err.println("[Map] " + e.getMessage());
                    }
                } else {
                    pendingJson = json;
                }
            });
        }, "map-geocode").start();
    }

    public class JavaBridge {
        public void openEvent(int id) {
            Platform.runLater(() -> { if (onEventClick != null) onEventClick.accept(id); });
        }
    }

    private void loadMap() {
        try {
            String js  = readRes("/leaflet.js");
            String css = readRes("/leaflet.css");
            if (js == null || css == null) {
                engine.loadContent(
                    "<html><body style='padding:20px;color:red;'>" +
                    "<h3>Erreur</h3><p>leaflet.js ou leaflet.css manquant dans resources/</p>" +
                    "</body></html>"
                );
                return;
            }

            // Écrire dans un fichier temporaire — permet les requêtes HTTPS depuis file://
            String html = buildHtml(js, css);
            tempHtmlFile = File.createTempFile("mindcare_map_", ".html");
            tempHtmlFile.deleteOnExit();
            Files.writeString(tempHtmlFile.toPath(), html, StandardCharsets.UTF_8);

            // Charger via file:// → JavaFX autorise les requêtes réseau
            engine.load(tempHtmlFile.toURI().toString());

        } catch (Exception e) {
            System.err.println("[Map] loadMap error: " + e.getMessage());
            // Fallback : loadContent
            try {
                String js  = readRes("/leaflet.js");
                String css = readRes("/leaflet.css");
                if (js != null && css != null)
                    engine.loadContent(buildHtml(js, css), "text/html");
            } catch (Exception ignored) {}
        }
    }

    private String readRes(String p) {
        try (InputStream is = getClass().getResourceAsStream(p)) {
            return is == null ? null : new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) { return null; }
    }

    private String buildHtml(String js, String css) {
        return
            "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
            "<style>" + css +
            "* { box-sizing:border-box; margin:0; padding:0; }" +
            "html, body { width:100%; height:100%; overflow:hidden; background:#f0f0f0; }" +
            "#map { position:absolute; top:0; left:0; right:0; bottom:0; }" +

            "#banner { position:absolute; top:10px; left:50%; transform:translateX(-50%);" +
            "  z-index:1000; background:rgba(255,255,255,0.96); border-radius:20px;" +
            "  padding:6px 18px; font-size:12px; font-weight:700; color:#0f2942;" +
            "  box-shadow:0 2px 12px rgba(0,0,0,0.2); white-space:nowrap; pointer-events:none;" +
            "  font-family:Arial,sans-serif; }" +

            "#legend { position:absolute; top:10px; right:10px; z-index:1000;" +
            "  background:rgba(255,255,255,0.96); border-radius:10px; padding:8px 12px;" +
            "  box-shadow:0 2px 8px rgba(0,0,0,0.15); font-size:11px; pointer-events:none;" +
            "  font-family:Arial,sans-serif; }" +
            ".lr { display:flex; align-items:center; gap:5px; margin:2px 0; color:#415a78; }" +
            ".ld { width:9px; height:9px; border-radius:50%; flex-shrink:0; }" +

            "#nearby { position:absolute; bottom:28px; left:10px; z-index:1000;" +
            "  background:rgba(255,255,255,0.96); border-radius:10px; padding:8px 12px;" +
            "  box-shadow:0 2px 8px rgba(0,0,0,0.15); max-width:220px;" +
            "  max-height:180px; overflow-y:auto; display:none; font-family:Arial,sans-serif; }" +
            "#nearby h4 { font-size:11px; font-weight:700; color:#0f2942; margin:0 0 5px 0; }" +
            ".ni { padding:4px 0; border-bottom:1px solid #eee; cursor:pointer; font-size:11px; }" +
            ".ni:last-child { border-bottom:none; }" +
            ".nn { color:#0f2942; font-weight:700; }" +
            ".nd { color:#9ab0cc; font-size:10px; }" +

            ".leaflet-popup-content-wrapper { border-radius:12px !important; }" +
            ".leaflet-popup-content { margin:0 !important; }" +
            ".pb { padding:10px 12px; min-width:190px; font-family:Arial,sans-serif; }" +
            ".pt { font-size:13px; font-weight:700; color:#0f2942; margin-bottom:5px; }" +
            ".pr { font-size:11px; color:#415a78; margin:2px 0; }" +
            ".pd { font-size:11px; font-weight:700; color:#2563eb; margin:3px 0; }" +
            ".pbtn { display:block; width:100%; margin-top:7px; padding:7px;" +
            "  background:#2563eb; color:white; border:none; border-radius:7px;" +
            "  font-size:11px; font-weight:700; cursor:pointer; }" +
            "</style></head><body>" +

            "<div id='map'></div>" +
            "<div id='banner'>Chargement de la carte...</div>" +
            "<div id='legend'>" +
            "  <div class='lr'><div class='ld' style='background:#3b82f6'></div><b>Vous</b></div>" +
            "  <div class='lr'><div class='ld' style='background:#22c55e'></div>&lt; 2 km</div>" +
            "  <div class='lr'><div class='ld' style='background:#f59e0b'></div>2-5 km</div>" +
            "  <div class='lr'><div class='ld' style='background:#ef4444'></div>&gt; 5 km</div>" +
            "</div>" +
            "<div id='nearby'><h4>Evenements proches</h4><div id='nl'></div></div>" +

            "<script>" + js + "\n" +

            "if(typeof map!=='undefined'&&map){try{map.remove();}catch(e){}}" +

            // Carte centrée sur Tunis
            "var map=L.map('map',{zoomControl:true,preferCanvas:true}).setView([36.8190,10.1658],12);" +

            // Tuiles via proxy local localhost:8080 — pas de blocage HTTPS dans JavaFX
            "L.tileLayer('http://localhost:8080/tiles/{z}/{x}/{y}.png',{" +
            "  attribution:'&copy; <a href=\"https://www.openstreetmap.org/copyright\">OpenStreetMap</a>'," +
            "  maxZoom:19," +
            "  updateWhenIdle:false," +
            "  keepBuffer:4" +
            "}).addTo(map);" +

            // invalidateSize multiples
            "setTimeout(function(){map.invalidateSize(true);},200);" +
            "setTimeout(function(){map.invalidateSize(true);},600);" +
            "setTimeout(function(){map.invalidateSize(true);},1200);" +
            "setTimeout(function(){map.invalidateSize(true);},2500);" +

            // Forcer rechargement des tuiles après invalidateSize
            "setTimeout(function(){" +
            "  map.invalidateSize(true);" +
            "  map.eachLayer(function(l){if(l._url)l.redraw();});" +
            "},3000);" +

            "var uLat=null,uLng=null,allM=[],allD=[],uMk=null,uCircle=null;" +

            "function mkIcon(c){" +
            "  var s='<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"26\" height=\"34\" viewBox=\"0 0 32 42\">'+" +
            "    '<path d=\"M16 0C7.163 0 0 7.163 0 16c0 10 16 26 16 26S32 26 32 16C32 7.163 24.837 0 16 0z\"'+" +
            "    ' fill=\"'+c+'\" stroke=\"white\" stroke-width=\"2.5\"/>'+" +
            "    '<circle cx=\"16\" cy=\"16\" r=\"6\" fill=\"white\" opacity=\"0.9\"/>'+" +
            "    '</svg>';" +
            "  return L.divIcon({html:s,iconSize:[26,34],iconAnchor:[13,34],popupAnchor:[0,-36],className:''});}" +

            "function userIcon(){" +
            "  var s='<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"32\" height=\"32\" viewBox=\"0 0 36 36\">'+" +
            "    '<circle cx=\"18\" cy=\"18\" r=\"15\" fill=\"#3b82f6\" stroke=\"white\" stroke-width=\"3\"/>'+" +
            "    '<circle cx=\"18\" cy=\"18\" r=\"6\" fill=\"white\"/>'+" +
            "    '</svg>';" +
            "  return L.divIcon({html:s,iconSize:[32,32],iconAnchor:[16,16],className:''});}" +

            "function dist(a,b,c,d){" +
            "  var R=6371,dL=(c-a)*Math.PI/180,dG=(d-b)*Math.PI/180;" +
            "  var x=Math.sin(dL/2)*Math.sin(dL/2)+Math.cos(a*Math.PI/180)*Math.cos(c*Math.PI/180)*Math.sin(dG/2)*Math.sin(dG/2);" +
            "  return R*2*Math.atan2(Math.sqrt(x),Math.sqrt(1-x));}" +

            "function distColor(km){return km<2?'#22c55e':km<5?'#f59e0b':'#ef4444';}" +
            "function openEvent(id){try{window.javaApp.openEvent(id);}catch(e){}}" +

            "function addMarker(ev,color){" +
            "  var km=uLat!==null?dist(uLat,uLng,ev.lat,ev.lng):null;" +
            "  var dh=km!==null?'<div class=\"pd\">'+km.toFixed(1)+' km de vous</div>':'';" +
            "  var p='<div class=\"pb\">'+" +
            "    '<div class=\"pt\">'+ev.titre+'</div>'+" +
            "    '<div class=\"pr\">&#128205; '+ev.lieu+'</div>'+" +
            "    '<div class=\"pr\">&#128197; '+ev.date+'</div>'+" +
            "    '<div class=\"pr\">'+(ev.remaining>0?ev.remaining+' place(s)':'Complet')+'</div>'+" +
            "    dh+" +
            "    '<button class=\"pbtn\" onclick=\"openEvent('+ev.id+')\">Voir l evenement</button>'+" +
            "    '</div>';" +
            "  var m=L.marker([ev.lat,ev.lng],{icon:mkIcon(color)})" +
            "    .bindPopup(p,{maxWidth:230}).addTo(map);" +
            "  allM.push(m);}" +

            "function loadMarkers(data){" +
            "  allM.forEach(function(m){map.removeLayer(m);}); allM=[];" +
            "  allD=data;" +
            "  if(!data||data.length===0){" +
            "    document.getElementById('banner').textContent='Aucun evenement';return;}" +
            "  document.getElementById('banner').textContent=data.length+' evenement(s)';" +
            "  data.forEach(function(ev){" +
            "    var c=uLat!==null?distColor(dist(uLat,uLng,ev.lat,ev.lng)):ev.color;" +
            "    addMarker(ev,c);});" +
            "  if(uLat===null&&allM.length>0){" +
            "    var b=L.featureGroup(allM).getBounds();" +
            "    if(b.isValid())map.fitBounds(b.pad(0.2),{maxZoom:14});}" +
            "  setTimeout(function(){map.invalidateSize(true);},300);}" +

            "function locateUser(){" +
            "  if(!navigator.geolocation){" +
            "    document.getElementById('banner').textContent='GPS non disponible';return;}" +
            "  navigator.geolocation.getCurrentPosition(function(pos){" +
            "    uLat=pos.coords.latitude; uLng=pos.coords.longitude;" +
            "    if(uMk){map.removeLayer(uMk);uMk=null;}" +
            "    if(uCircle){map.removeLayer(uCircle);uCircle=null;}" +
            "    uMk=L.marker([uLat,uLng],{icon:userIcon(),zIndexOffset:1000})" +
            "      .bindPopup('<div class=\"pb\"><div class=\"pt\">Vous etes ici</div></div>')" +
            "      .addTo(map);" +
            "    uCircle=L.circle([uLat,uLng],{radius:2000,color:'#3b82f6'," +
            "      fillColor:'#3b82f6',fillOpacity:0.07,weight:1.5}).addTo(map);" +
            "    document.getElementById('banner').textContent='Position detectee';" +
            "    allM.forEach(function(m){map.removeLayer(m);}); allM=[];" +
            "    var s=allD.slice().sort(function(a,b){" +
            "      return dist(uLat,uLng,a.lat,a.lng)-dist(uLat,uLng,b.lat,b.lng);});" +
            "    s.forEach(function(ev){addMarker(ev,distColor(dist(uLat,uLng,ev.lat,ev.lng)));});" +
            "    var nb=s.filter(function(ev){return dist(uLat,uLng,ev.lat,ev.lng)<=5;});" +
            "    if(nb.length>0){" +
            "      var panel=document.getElementById('nearby');" +
            "      var list=document.getElementById('nl');" +
            "      panel.style.display='block'; list.innerHTML='';" +
            "      nb.slice(0,5).forEach(function(ev){" +
            "        var km=dist(uLat,uLng,ev.lat,ev.lng).toFixed(1);" +
            "        var item=document.createElement('div'); item.className='ni';" +
            "        item.innerHTML='<div class=\"nn\">'+ev.titre+'</div><div class=\"nd\">'+km+' km</div>';" +
            "        item.onclick=function(){openEvent(ev.id);};" +
            "        list.appendChild(item);});}" +
            "    map.setView([uLat,uLng],13);" +
            "  },function(){" +
            "    document.getElementById('banner').textContent='Position non disponible';}" +
            "  ,{timeout:8000,maximumAge:60000,enableHighAccuracy:false});}" +

            "locateUser();" +
            "</script></body></html>";
    }
}
