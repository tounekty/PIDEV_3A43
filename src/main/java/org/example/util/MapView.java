package org.example.util;

import javafx.application.Platform;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.example.event.Event;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Carte interactive Leaflet (admin).
 * Affiche les événements avec un score de lieu (centralité).
 */
public class MapView extends VBox {

    private final WebView    webView;
    private final WebEngine  engine;
    private final MapService mapService = new MapService();

    private String  pendingJson = null;
    private boolean ready       = false;

    public MapView() {
        webView = new WebView();
        webView.setPrefWidth(Double.MAX_VALUE);
        webView.setPrefHeight(Double.MAX_VALUE);
        webView.setMinHeight(400);
        webView.setContextMenuEnabled(false);
        VBox.setVgrow(webView, Priority.ALWAYS);
        getChildren().add(webView);
        VBox.setVgrow(this, Priority.ALWAYS);

        engine = webView.getEngine();
        engine.setUserAgent(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        );

        engine.getLoadWorker().stateProperty().addListener((obs, o, n) -> {
            if (n != javafx.concurrent.Worker.State.SUCCEEDED) return;
            ready = true;
            engine.executeScript(
                "setTimeout(function(){if(typeof map!=='undefined')map.invalidateSize(true);},200);" +
                "setTimeout(function(){if(typeof map!=='undefined')map.invalidateSize(true);},600);" +
                "setTimeout(function(){if(typeof map!=='undefined')map.invalidateSize(true);},1400);" +
                "setTimeout(function(){if(typeof map!=='undefined')map.invalidateSize(true);},2500);"
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

        webView.widthProperty().addListener((obs, o, n) -> triggerInvalidate());
        webView.heightProperty().addListener((obs, o, n) -> triggerInvalidate());

        loadMap();
    }

    private void triggerInvalidate() {
        if (!ready) return;
        Platform.runLater(() -> {
            try { engine.executeScript("if(typeof map!=='undefined')map.invalidateSize(true);"); }
            catch (Exception ignored) {}
        });
    }

    public void loadEvents(List<Event> events, Map<Integer, Integer> resCounts) {
        new Thread(() -> {
            String json = mapService.buildMarkersJson(events, resCounts);
            Platform.runLater(() -> {
                if (ready) {
                    try {
                        engine.executeScript("loadMarkers(" + json + ")");
                        engine.executeScript(
                            "setTimeout(function(){if(typeof map!=='undefined')map.invalidateSize(true);},400);"
                        );
                    } catch (Exception e) {
                        System.err.println("[MapView] " + e.getMessage());
                    }
                } else {
                    pendingJson = json;
                }
            });
        }, "map-geocode").start();
    }

    private void loadMap() {
        try {
            String js  = readRes("/leaflet.js");
            String css = readRes("/leaflet.css");
            if (js == null || css == null) {
                engine.loadContent("<html><body style='padding:20px;color:red;'>Leaflet manquant.</body></html>");
                return;
            }
            engine.loadContent(buildHtml(js, css), "text/html");
        } catch (Exception e) {
            System.err.println("[MapView] " + e.getMessage());
        }
    }

    private String readRes(String p) {
        try (InputStream is = getClass().getResourceAsStream(p)) {
            return is == null ? null : new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) { return null; }
    }

    private String buildHtml(String js, String css) {
        return
            "<!DOCTYPE html><html><head><meta charset='UTF-8'><style>" + css +
            "* { box-sizing:border-box; margin:0; padding:0; }" +
            "html,body { width:100%; height:100%; overflow:hidden; background:#e8e8e8; }" +
            "#map { position:absolute; top:0; left:0; right:0; bottom:0; }" +

            "#ctr { position:absolute; top:10px; left:10px; z-index:1000;" +
            "  background:rgba(255,255,255,.95); border-radius:10px; padding:7px 13px;" +
            "  box-shadow:0 2px 8px rgba(0,0,0,.15); font-size:12px; font-weight:700;" +
            "  color:#0f2942; font-family:Arial,sans-serif; }" +

            ".legend { position:absolute; bottom:28px; left:10px; z-index:1000;" +
            "  background:rgba(255,255,255,.95); border-radius:10px; padding:10px 14px;" +
            "  box-shadow:0 2px 8px rgba(0,0,0,.15); font-family:Arial,sans-serif; }" +
            ".legend h4 { font-size:11px; font-weight:700; color:#0f2942; margin:0 0 6px 0; }" +
            ".li { display:flex; align-items:center; gap:6px; margin:3px 0; font-size:11px; color:#415a78; }" +
            ".ld { width:10px; height:10px; border-radius:50%; flex-shrink:0; }" +

            "#rec { position:absolute; top:10px; right:10px; z-index:1000;" +
            "  background:rgba(255,255,255,.95); border-radius:10px; padding:12px 14px;" +
            "  box-shadow:0 2px 8px rgba(0,0,0,.15); max-width:240px; display:none;" +
            "  font-family:Arial,sans-serif; }" +
            "#rec h4 { font-size:11px; font-weight:700; color:#0f2942; margin:0 0 5px 0; }" +
            "#rc { font-size:11px; color:#415a78; line-height:1.5; }" +

            ".leaflet-popup-content-wrapper { border-radius:12px !important; }" +
            ".leaflet-popup-content { margin:0 !important; }" +
            ".pb { padding:10px 12px; min-width:190px; font-family:Arial,sans-serif; }" +
            ".pt { font-size:13px; font-weight:700; color:#0f2942; margin-bottom:5px; }" +
            ".pr { font-size:11px; color:#415a78; margin:2px 0; }" +
            ".ps { display:inline-block; padding:2px 7px; border-radius:999px;" +
            "  font-size:10px; font-weight:700; margin-top:4px; }" +
            ".sh { background:#dcfce7; color:#15803d; }" +
            ".sm { background:#fef9c3; color:#b45309; }" +
            ".sl { background:#fee2e2; color:#c63d48; }" +
            "</style></head><body>" +

            "<div id='map'></div>" +
            "<div id='ctr'>Chargement de la carte...</div>" +
            "<div class='legend'>" +
            "  <h4>Score de lieu</h4>" +
            "  <div class='li'><div class='ld' style='background:#22c55e'></div>Central (&ge;70)</div>" +
            "  <div class='li'><div class='ld' style='background:#f59e0b'></div>Correct (40-70)</div>" +
            "  <div class='li'><div class='ld' style='background:#ef4444'></div>Eloigne (&lt;40)</div>" +
            "</div>" +
            "<div id='rec'><h4>Recommandation</h4><div id='rc'>Cliquez sur un marqueur.</div></div>" +

            "<script>" + js + "\n" +

            "if(typeof map!=='undefined'&&map){try{map.remove();}catch(e){}}" +
            "var map=L.map('map',{zoomControl:true}).setView([36.8190,10.1658],12);" +

            // OSM principal
            "var osmLayer=L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{" +
            "  attribution:'© OpenStreetMap contributors',maxZoom:19,subdomains:'abc',crossOrigin:true});" +

            // CARTO fallback
            "var cartoLayer=L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png',{" +
            "  attribution:'© OpenStreetMap © CARTO',subdomains:'abcd',maxZoom:20,crossOrigin:true});" +

            "var tileErrors=0;" +
            "osmLayer.on('tileerror',function(){" +
            "  tileErrors++;" +
            "  if(tileErrors===3){map.removeLayer(osmLayer);cartoLayer.addTo(map);}});" +
            "osmLayer.addTo(map);" +

            "setTimeout(function(){map.invalidateSize(true);},150);" +
            "setTimeout(function(){map.invalidateSize(true);},500);" +
            "setTimeout(function(){map.invalidateSize(true);},1000);" +
            "setTimeout(function(){map.invalidateSize(true);},2000);" +

            "var markers=[],heat=[];" +

            "function mkIcon(c){" +
            "  var s='<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"28\" height=\"36\" viewBox=\"0 0 32 42\">'+" +
            "    '<path d=\"M16 0C7.163 0 0 7.163 0 16c0 10 16 26 16 26S32 26 32 16C32 7.163 24.837 0 16 0z\"'+" +
            "    ' fill=\"'+c+'\" stroke=\"white\" stroke-width=\"2\"/>'+" +
            "    '<circle cx=\"16\" cy=\"16\" r=\"6\" fill=\"white\" opacity=\"0.9\"/></svg>';" +
            "  return L.divIcon({html:s,iconSize:[28,36],iconAnchor:[14,36],popupAnchor:[0,-38],className:''});}" +

            "function loadMarkers(data){" +
            "  markers.forEach(function(m){map.removeLayer(m);});" +
            "  heat.forEach(function(c){map.removeLayer(c);});" +
            "  markers=[];heat=[];" +
            "  if(!data||!data.length){document.getElementById('ctr').textContent='Aucun evenement';return;}" +
            "  document.getElementById('ctr').textContent=data.length+' evenement(s)';" +
            "  var best=data.reduce(function(a,b){return a.score>b.score?a:b;});" +
            "  data.forEach(function(ev){" +
            "    var h=L.circle([ev.lat,ev.lng],{radius:300,color:ev.color,fillColor:ev.color,fillOpacity:0.12,weight:0}).addTo(map);" +
            "    heat.push(h);" +
            "    var sc=ev.score>=70?'sh':ev.score>=40?'sm':'sl';" +
            "    var badge=ev.id===best.id?' ★':'';" +
            "    var p='<div class=\"pb\">'+" +
            "      '<div class=\"pt\">'+ev.titre+'</div>'+" +
            "      '<div class=\"pr\">📍 '+ev.lieu+'</div>'+" +
            "      '<div class=\"pr\">📅 '+ev.date+'</div>'+" +
            "      '<div class=\"pr\">'+(ev.remaining>0?ev.remaining+' place(s)':'Complet')+'</div>'+" +
            "      '<span class=\"ps '+sc+'\">Score: '+ev.score+'/100'+badge+'</span>'+" +
            "      '</div>';" +
            "    var m=L.marker([ev.lat,ev.lng],{icon:mkIcon(ev.color)}).bindPopup(p,{maxWidth:240}).addTo(map);" +
            "    m.on('click',function(){showRec(ev,best);});" +
            "    markers.push(m);});" +
            "  if(markers.length>0){" +
            "    var b=L.featureGroup(markers).getBounds();" +
            "    if(b.isValid())map.fitBounds(b.pad(0.2),{maxZoom:14});}" +
            "  setTimeout(function(){map.invalidateSize(true);},300);}" +

            "function showRec(ev,best){" +
            "  var r=document.getElementById('rec'),c=document.getElementById('rc');" +
            "  r.style.display='block';" +
            "  var h=ev.id===best.id?'<div style=\"background:#dcfce7;border-radius:5px;padding:4px 7px;margin-bottom:5px;\"><b style=\"color:#15803d;\">Meilleur lieu</b></div>':'';" +
            "  h+='<b>'+ev.titre+'</b><br>'+ev.lieu+'<br>Score: <b>'+ev.score+'/100</b><br>'+" +
            "    (ev.score>=70?'Lieu central':ev.score>=40?'Lieu correct':'Lieu eloigne');" +
            "  c.innerHTML=h;}" +

            "</script></body></html>";
    }
}
