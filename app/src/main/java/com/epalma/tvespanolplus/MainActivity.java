package com.epalma.tvespanolplus;

import android.app.*;
import android.app.PictureInPictureParams;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.*;
import android.os.*;
import android.util.Rational;
import android.view.*;
import android.widget.*;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

public class MainActivity extends Activity {
  static final String REMOTE="https://raw.githubusercontent.com/jlpalmav-cpu/tv-espanol-plus/main/TV_Espanol_Plus_VERIFICADA.m3u";
  static final int OPEN_M3U=7001;
  final ExecutorService io=Executors.newSingleThreadExecutor();
  final List<Channel> all=new ArrayList<>(),shown=new ArrayList<>();
  final LinkedHashSet<String> favorites=new LinkedHashSet<>();
  final ArrayDeque<String> recents=new ArrayDeque<>();
  ArrayAdapter<String> adapter; ExoPlayer player; PlayerView playerView; Channel current,previous;
  ListView list; EditText search; TextView title,status,count,sourceText; Button favButton;
  String mode="all"; int retries=0; boolean autoPip=true;

  @Override public void onCreate(Bundle b){super.onCreate(b);loadPrefs();buildUi();initPlayer();bind();loadSavedSource();}

  int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
  GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
  TextView text(String s,int sp){TextView v=new TextView(this);v.setText(s);v.setTextColor(Color.WHITE);v.setTextSize(sp);v.setGravity(Gravity.CENTER_VERTICAL);v.setPadding(dp(12),0,dp(12),0);return v;}
  Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setTextSize(13);b.setFocusable(true);b.setBackground(bg(Color.rgb(24,43,63),10));b.setOnFocusChangeListener((v,f)->{v.setScaleX(f?1.04f:1f);v.setScaleY(f?1.04f:1f);v.setAlpha(f?1f:.9f);});return b;}
  LinearLayout.LayoutParams w1(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);p.setMargins(dp(3),0,dp(3),0);return p;}

  void buildUi(){
    LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(10),dp(8),dp(10),dp(8));root.setBackgroundColor(Color.rgb(5,13,24));
    LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
    TextView logo=text("▣  TV Español+",26);logo.setTypeface(null,android.graphics.Typeface.BOLD);top.addView(logo,new LinearLayout.LayoutParams(0,dp(52),1));
    sourceText=text("Lista principal",12);sourceText.setTextColor(Color.LTGRAY);sourceText.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);top.addView(sourceText,new LinearLayout.LayoutParams(dp(250),dp(52)));
    Button settings=button("⚙ Configuración");top.addView(settings,new LinearLayout.LayoutParams(dp(150),dp(46)));root.addView(top);

    LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.HORIZONTAL);root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
    LinearLayout left=new LinearLayout(this);left.setOrientation(LinearLayout.VERTICAL);left.setPadding(0,0,dp(10),0);body.addView(left,new LinearLayout.LayoutParams(dp(350),-1));
    search=new EditText(this);search.setHint("Buscar canal, país o categoría…");search.setSingleLine(true);search.setTextColor(Color.WHITE);search.setHintTextColor(Color.rgb(155,169,184));search.setBackground(bg(Color.rgb(16,31,48),10));search.setPadding(dp(14),0,dp(14),0);left.addView(search,new LinearLayout.LayoutParams(-1,dp(48)));
    LinearLayout filters=new LinearLayout(this);filters.setPadding(0,dp(6),0,dp(4));Button allB=button("Todos"),favB=button("★ Favoritos"),recB=button("↻ Recientes");filters.addView(allB,w1());filters.addView(favB,w1());filters.addView(recB,w1());left.addView(filters,new LinearLayout.LayoutParams(-1,dp(54)));
    count=text("Cargando…",12);count.setTextColor(Color.rgb(160,180,200));left.addView(count,new LinearLayout.LayoutParams(-1,dp(32)));
    list=new ListView(this);list.setDividerHeight(1);list.setDivider(new android.graphics.drawable.ColorDrawable(Color.rgb(26,43,59)));list.setBackground(bg(Color.rgb(8,20,34),10));left.addView(list,new LinearLayout.LayoutParams(-1,0,1));

    LinearLayout right=new LinearLayout(this);right.setOrientation(LinearLayout.VERTICAL);body.addView(right,new LinearLayout.LayoutParams(0,-1,1));
    title=text("Seleccione un canal",21);title.setTypeface(null,android.graphics.Typeface.BOLD);right.addView(title,new LinearLayout.LayoutParams(-1,dp(46)));
    playerView=new PlayerView(this);playerView.setFocusable(true);playerView.setUseController(true);right.addView(playerView,new LinearLayout.LayoutParams(-1,0,1));
    status=text("Listo",13);status.setTextColor(Color.rgb(176,195,211));right.addView(status,new LinearLayout.LayoutParams(-1,dp(32)));
    LinearLayout controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER);Button prev=button("◀ Anterior"),next=button("Siguiente ▶"),retry=button("↻ Reconectar"),more=button("⋮ Más");favButton=button("☆ Favorito");
    for(Button x:new Button[]{prev,favButton,next,retry,more})controls.addView(x,w1());right.addView(controls,new LinearLayout.LayoutParams(-1,dp(56)));

    setContentView(root);
    player=new ExoPlayer.Builder(this).build();playerView.setPlayer(player);
    allB.setOnClickListener(v->{mode="all";filter();});favB.setOnClickListener(v->{mode="fav";filter();});recB.setOnClickListener(v->{mode="recent";filter();});
    prev.setOnClickListener(v->{if(previous!=null)play(previous);else step(-1);});next.setOnClickListener(v->step(1));retry.setOnClickListener(v->{retries=0;restart();});favButton.setOnClickListener(v->toggleFavorite());
    more.setOnClickListener(v->showMoreMenu());settings.setOnClickListener(v->showSettings());
  }

  void initPlayer(){player.addListener(new Player.Listener(){
    @Override public void onPlaybackStateChanged(int s){if(s==Player.STATE_READY){retries=0;status.setText("● En vivo");}else if(s==Player.STATE_BUFFERING)status.setText("Cargando…");}
    @Override public void onPlayerError(PlaybackException e){autoRetry();}
  });}
  void bind(){adapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,new ArrayList<String>()){
    @Override public View getView(int p,View c,ViewGroup parent){TextView v=(TextView)super.getView(p,c,parent);v.setTextColor(Color.WHITE);v.setTextSize(15);v.setPadding(dp(12),dp(7),dp(8),dp(7));v.setMinHeight(dp(54));v.setFocusable(false);return v;}
  };list.setAdapter(adapter);list.setOnItemClickListener((p,v,pos,id)->play(shown.get(pos)));search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void onTextChanged(CharSequence s,int a,int b,int c){filter();}public void afterTextChanged(android.text.Editable e){}});}

  void loadSavedSource(){SharedPreferences p=getSharedPreferences("tvplus",MODE_PRIVATE);String uri=p.getString("m3u_uri",""),url=p.getString("m3u_url","");
    if(!uri.isEmpty()){try{loadFromUri(Uri.parse(uri),"M3U local");return;}catch(Exception ignored){}}
    if(!url.isEmpty()){loadFromUrl(url,"M3U personalizada");return;}
    loadDefault();
  }
  void loadDefault(){count.setText("Cargando lista…");sourceText.setText("Lista principal");io.submit(()->{List<Channel> parsed=null;String src="GitHub";try{URLConnection c=new URL(REMOTE).openConnection();c.setConnectTimeout(4000);c.setReadTimeout(6500);c.setRequestProperty("User-Agent","TV-Espanol-Plus/1.7");parsed=parse(c.getInputStream());}catch(Exception e){src="respaldo local";}if(parsed==null||parsed.size()<100){try{parsed=parse(getAssets().open("TV_Espanol_Plus_VERIFICADA.m3u"));src="respaldo local";}catch(Exception e){parsed=Collections.emptyList();}}applyChannels(parsed,src);});}
  void loadFromUrl(String url,String label){count.setText("Cargando M3U…");sourceText.setText(label);io.submit(()->{try{URLConnection c=new URL(url).openConnection();c.setConnectTimeout(5000);c.setReadTimeout(9000);c.setRequestProperty("User-Agent","TV-Espanol-Plus/1.7");List<Channel> p=parse(c.getInputStream());if(p.isEmpty())throw new IOException("Lista vacía");applyChannels(p,label);}catch(Exception e){runOnUiThread(()->{status.setText("No se pudo cargar la lista URL");toast("Error M3U: "+e.getMessage());});}});}
  void loadFromUri(Uri uri,String label){sourceText.setText(label);count.setText("Leyendo archivo M3U…");io.submit(()->{try(InputStream in=getContentResolver().openInputStream(uri)){List<Channel> p=parse(in);if(p.isEmpty())throw new IOException("Lista vacía");applyChannels(p,label);}catch(Exception e){runOnUiThread(()->toast("No se pudo leer M3U: "+e.getMessage()));}});}
  void applyChannels(List<Channel> p,String label){runOnUiThread(()->{all.clear();all.addAll(p);mode="all";filter();sourceText.setText(label);count.setText(all.size()+" canales · "+label);});}

  void filter(){if(adapter==null)return;String q=search.getText().toString().trim().toLowerCase(Locale.ROOT);shown.clear();Set<String> rs=new HashSet<>(recents);for(Channel c:all){boolean m=mode.equals("all")||(mode.equals("fav")&&favorites.contains(c.id))||(mode.equals("recent")&&rs.contains(c.id));if(m&&(q.isEmpty()||c.search.contains(q)))shown.add(c);}if(mode.equals("recent")){final Map<String,Integer> rank=new HashMap<>();int i=0;for(String id:recents)rank.put(id,i++);Collections.sort(shown,(a,b)->Integer.compare(rank.containsKey(a.id)?rank.get(a.id):9999,rank.containsKey(b.id)?rank.get(b.id):9999));}List<String> names=new ArrayList<>();for(Channel c:shown)names.add(c.name+"\n"+c.group);adapter.clear();adapter.addAll(names);adapter.notifyDataSetChanged();count.setText(shown.size()+" de "+all.size()+" canales");}
  void play(Channel c){if(current!=null&&!current.id.equals(c.id))previous=current;current=c;retries=0;title.setText(c.name+"  ·  "+c.group);addRecent(c.id);updateFav();status.setText("Conectando…");player.setMediaItem(new MediaItem.Builder().setUri(c.url).setMediaId(c.id).build());player.prepare();player.play();}
  void restart(){if(current==null)return;status.setText("Reconectando…");player.setMediaItem(new MediaItem.Builder().setUri(current.url).setMediaId(current.id).build());player.prepare();player.play();}
  void autoRetry(){if(current==null)return;if(retries<3){retries++;status.setText("Reconectando "+retries+"/3…");new Handler(getMainLooper()).postDelayed(this::restart,retries*1000L);}else status.setText("Canal sin respuesta");}
  void step(int d){if(all.isEmpty())return;int i=current==null?-1:all.indexOf(current);i=(i+d+all.size())%all.size();play(all.get(i));}
  void toggleFavorite(){if(current==null)return;if(!favorites.add(current.id))favorites.remove(current.id);savePrefs();updateFav();if(mode.equals("fav"))filter();}
  void updateFav(){favButton.setText(current!=null&&favorites.contains(current.id)?"★ Favorito":"☆ Favorito");}
  void addRecent(String id){recents.remove(id);recents.addFirst(id);while(recents.size()>30)recents.removeLast();savePrefs();}
  void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}

  void showMoreMenu(){String[] items={"Audio","Subtítulos","Picture in Picture","Cambiar lista M3U","Configuración","Diagnóstico","Salir"};new AlertDialog.Builder(this).setTitle("Más opciones").setItems(items,(d,w)->{switch(w){case 0:showTracks(C.TRACK_TYPE_AUDIO);break;case 1:showTracks(C.TRACK_TYPE_TEXT);break;case 2:enterPip();break;case 3:showM3uMenu();break;case 4:showSettings();break;case 5:diagnostics();break;case 6:confirmExit();break;}}).show();}
  void showTracks(int type){Tracks t=player.getCurrentTracks();final List<TrackSelectionOverride> ovs=new ArrayList<>();final List<String> labels=new ArrayList<>();if(type==C.TRACK_TYPE_TEXT)labels.add("Desactivados");for(Tracks.Group g:t.getGroups()){if(g.getType()!=type)continue;for(int i=0;i<g.length;i++){Format f=g.getTrackFormat(i);String l=f.label!=null?f.label:(f.language!=null?f.language:(type==C.TRACK_TYPE_AUDIO?"Audio ":"Subtítulo ")+(labels.size()+1));labels.add(l);ovs.add(new TrackSelectionOverride(g.getMediaTrackGroup(),i));}}
    if(labels.isEmpty()||(type==C.TRACK_TYPE_TEXT&&labels.size()==1)){toast(type==C.TRACK_TYPE_AUDIO?"No hay pistas de audio adicionales":"Este canal no ofrece subtítulos");return;}
    new AlertDialog.Builder(this).setTitle(type==C.TRACK_TYPE_AUDIO?"Audio":"Subtítulos").setItems(labels.toArray(new String[0]),(d,w)->{TrackSelectionParameters.Builder b=player.getTrackSelectionParameters().buildUpon();if(type==C.TRACK_TYPE_TEXT&&w==0){b.setTrackTypeDisabled(C.TRACK_TYPE_TEXT,true);}else{int x=type==C.TRACK_TYPE_TEXT?w-1:w;b.setTrackTypeDisabled(type,false);b.setOverrideForType(ovs.get(x));}player.setTrackSelectionParameters(b.build());}).show();
  }
  void enterPip(){if(Build.VERSION.SDK_INT<26){toast("Picture in Picture requiere Android 8 o superior");return;}if(current==null){toast("Seleccione un canal primero");return;}try{PictureInPictureParams p=new PictureInPictureParams.Builder().setAspectRatio(new Rational(16,9)).build();enterPictureInPictureMode(p);}catch(Exception e){toast("PiP no disponible en este dispositivo");}}
  @Override public void onUserLeaveHint(){super.onUserLeaveHint();if(autoPip&&player!=null&&player.isPlaying()&&Build.VERSION.SDK_INT>=26)enterPip();}

  void showM3uMenu(){String[] x={"Usar lista TV Español+ (1,733 canales)","Abrir archivo M3U","Cargar M3U desde URL"};new AlertDialog.Builder(this).setTitle("Mis listas M3U").setItems(x,(d,w)->{if(w==0){getSharedPreferences("tvplus",MODE_PRIVATE).edit().remove("m3u_uri").remove("m3u_url").apply();loadDefault();}else if(w==1)pickM3u();else askUrl();}).show();}
  void pickM3u(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");startActivityForResult(i,OPEN_M3U);}
  @Override protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(req==OPEN_M3U&&res==RESULT_OK&&data!=null&&data.getData()!=null){Uri u=data.getData();try{getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}getSharedPreferences("tvplus",MODE_PRIVATE).edit().putString("m3u_uri",u.toString()).remove("m3u_url").apply();loadFromUri(u,"M3U local");}}
  void askUrl(){final EditText e=new EditText(this);e.setHint("https://servidor/lista.m3u");e.setSingleLine(true);new AlertDialog.Builder(this).setTitle("URL de lista M3U").setView(e).setNegativeButton("Cancelar",null).setPositiveButton("Cargar",(d,w)->{String u=e.getText().toString().trim();if(u.startsWith("http://")||u.startsWith("https://")){getSharedPreferences("tvplus",MODE_PRIVATE).edit().putString("m3u_url",u).remove("m3u_uri").apply();loadFromUrl(u,"M3U URL");}else toast("Ingrese una URL válida");}).show();}

  void showSettings(){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(22),dp(8),dp(22),0);CheckBox pip=new CheckBox(this);pip.setText("Picture in Picture automático");pip.setChecked(autoPip);box.addView(pip);Button lists=button("Administrar listas M3U");box.addView(lists,new LinearLayout.LayoutParams(-1,dp(50)));Button aud=button("Seleccionar audio");box.addView(aud,new LinearLayout.LayoutParams(-1,dp(50)));Button sub=button("Seleccionar subtítulos");box.addView(sub,new LinearLayout.LayoutParams(-1,dp(50)));TextView about=text("TV Español+ v1.7.0\nMedia3 / ExoPlayer · Android TV / Google TV",13);box.addView(about,new LinearLayout.LayoutParams(-1,dp(65)));AlertDialog dlg=new AlertDialog.Builder(this).setTitle("Configuración").setView(box).setNegativeButton("Cerrar",null).setPositiveButton("Guardar",(d,w)->{autoPip=pip.isChecked();savePrefs();}).create();lists.setOnClickListener(v->{dlg.dismiss();showM3uMenu();});aud.setOnClickListener(v->showTracks(C.TRACK_TYPE_AUDIO));sub.setOnClickListener(v->showTracks(C.TRACK_TYPE_TEXT));dlg.show();}
  void diagnostics(){ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);new AlertDialog.Builder(this).setTitle("Diagnóstico TV Español+").setMessage("Versión: 1.7.0\nCanales cargados: "+all.size()+"\nFavoritos: "+favorites.size()+"\nRecientes: "+recents.size()+"\nRed activa: "+(cm.getActiveNetwork()!=null?"Sí":"No")+"\nPiP automático: "+(autoPip?"Sí":"No")+"\nReproductor: Media3 / ExoPlayer").setPositiveButton("Cerrar",null).show();}
  void confirmExit(){new AlertDialog.Builder(this).setTitle("Salir de TV Español+").setMessage("¿Desea cerrar la aplicación?").setNegativeButton("Cancelar",null).setPositiveButton("Salir",(d,w)->finishAndRemoveTask()).show();}

  void loadPrefs(){SharedPreferences p=getSharedPreferences("tvplus",MODE_PRIVATE);favorites.addAll(p.getStringSet("favorites",Collections.emptySet()));String r=p.getString("recents","");if(!r.isEmpty())for(String s:r.split("\\|"))if(!s.isEmpty())recents.add(s);autoPip=p.getBoolean("auto_pip",true);}
  void savePrefs(){getSharedPreferences("tvplus",MODE_PRIVATE).edit().putStringSet("favorites",new HashSet<>(favorites)).putString("recents",joinRecents()).putBoolean("auto_pip",autoPip).apply();}
  String joinRecents(){StringBuilder b=new StringBuilder();for(String id:recents){if(b.length()>0)b.append("|");b.append(id);}return b.toString();}

  @Override public boolean dispatchKeyEvent(KeyEvent e){if(e.getAction()==KeyEvent.ACTION_DOWN){if(e.getKeyCode()==KeyEvent.KEYCODE_MEDIA_NEXT){step(1);return true;}if(e.getKeyCode()==KeyEvent.KEYCODE_MEDIA_PREVIOUS){step(-1);return true;}}return super.dispatchKeyEvent(e);}
  @Override public void onBackPressed(){if(Build.VERSION.SDK_INT>=26&&isInPictureInPictureMode())return;confirmExit();}
  @Override protected void onDestroy(){super.onDestroy();if(player!=null)player.release();io.shutdownNow();}

  static final Pattern GROUP=Pattern.compile("group-title=\\\"([^\\\"]*)\\\""),ID=Pattern.compile("tvg-id=\\\"([^\\\"]*)\\\"");
  static List<Channel> parse(InputStream in)throws IOException{ArrayList<Channel> out=new ArrayList<>();if(in==null)return out;try(BufferedReader br=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){String line,meta=null;while((line=br.readLine())!=null){line=line.trim();if(line.startsWith("#EXTINF:"))meta=line;else if(!line.isEmpty()&&!line.startsWith("#")&&meta!=null){String name=meta.contains(",")?meta.substring(meta.lastIndexOf(',')+1).trim():"Canal";Matcher gm=GROUP.matcher(meta),im=ID.matcher(meta);String group=gm.find()?gm.group(1):"Otros";String id=im.find()&&im.group(1).trim().length()>0?im.group(1):sha1(name+"|"+line);out.add(new Channel(id,name,group,line));meta=null;}}}return out;}
  static String sha1(String s){try{byte[] b=MessageDigest.getInstance("SHA-1").digest(s.getBytes(StandardCharsets.UTF_8));StringBuilder x=new StringBuilder();for(byte v:b)x.append(String.format("%02x",v));return x.toString();}catch(Exception e){return Integer.toHexString(s.hashCode());}}
  static final class Channel{final String id,name,group,url,search;Channel(String i,String n,String g,String u){id=i;name=n;group=g;url=u;search=(n+" "+g).toLowerCase(Locale.ROOT);}}
}
