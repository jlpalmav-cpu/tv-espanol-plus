package com.epalma.tvespanolplus;

import android.app.*;
import android.annotation.SuppressLint;
import android.os.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.*;
import android.util.Rational;
import android.view.*;
import android.widget.*;
import androidx.media3.common.*;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.media3.ui.TrackSelectionDialogBuilder;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

public class MainActivity extends Activity {
  static final int REQ_M3U=4101;
  static final String OFFICIAL="https://raw.githubusercontent.com/jlpalmav-cpu/tv-espanol-plus/main/TV_Espanol_Plus_VERIFICADA.m3u";
  static final String PREFS="tvplus17";

  final ExecutorService io=Executors.newSingleThreadExecutor();
  final List<Channel> all=new ArrayList<>(), shown=new ArrayList<>();
  final LinkedHashSet<String> favorites=new LinkedHashSet<>();
  final ArrayDeque<String> recents=new ArrayDeque<>();
  ExoPlayer player; PlayerView playerView; Channel current, previous;
  ChannelAdapter adapter; ListView list; EditText search; TextView title,status,count,sourceLabel;
  Button favButton; LinearLayout leftPanel,topHeader,bottomBar; String mode="all"; int retries=0;

  @Override public void onCreate(Bundle b){super.onCreate(b);loadPrefs();buildUi();initPlayer();bindSearch();loadChannels();}

  int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
  GradientDrawable bg(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
  TextView label(String s,int sp){TextView v=new TextView(this);v.setText(s);v.setTextColor(Color.WHITE);v.setTextSize(sp);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
  Button button(String s){
    Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(12);b.setAllCaps(false);b.setSingleLine(true);b.setFocusable(true);b.setPadding(dp(12),0,dp(12),0);b.setBackground(bg(Color.rgb(25,45,68),10));
    b.setOnFocusChangeListener((v,has)->v.setBackground(bg(has?Color.rgb(45,169,255):Color.rgb(25,45,68),10)));
    return b;
  }
  LinearLayout.LayoutParams ctrlLp(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(112),dp(48));p.setMargins(dp(4),dp(4),dp(4),dp(4));return p;}

  void buildUi(){
    final int BG=Color.rgb(6,15,28), PANEL=Color.rgb(11,27,43), CARD=Color.rgb(18,38,60);
    LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);root.setPadding(dp(10),dp(8),dp(10),dp(8));

    topHeader=new LinearLayout(this);topHeader.setOrientation(LinearLayout.HORIZONTAL);topHeader.setGravity(Gravity.CENTER_VERTICAL);topHeader.setPadding(dp(8),0,dp(8),0);
    TextView brand=label("TV Español+",26);brand.setTypeface(null,android.graphics.Typeface.BOLD);topHeader.addView(brand,new LinearLayout.LayoutParams(0,dp(52),1));
    sourceLabel=label("Lista oficial",12);sourceLabel.setTextColor(Color.LTGRAY);sourceLabel.setGravity(Gravity.CENTER);topHeader.addView(sourceLabel,new LinearLayout.LayoutParams(dp(190),dp(42)));
    Button settings=button("⚙ Configuración");topHeader.addView(settings,new LinearLayout.LayoutParams(dp(150),dp(44)));root.addView(topHeader,new LinearLayout.LayoutParams(-1,dp(54)));

    LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.HORIZONTAL);root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
    leftPanel=new LinearLayout(this);leftPanel.setOrientation(LinearLayout.VERTICAL);leftPanel.setPadding(dp(8),dp(8),dp(8),dp(8));leftPanel.setBackground(bg(PANEL,14));body.addView(leftPanel,new LinearLayout.LayoutParams(0,-1,0.33f));
    search=new EditText(this);search.setHint("Buscar canal, país o categoría…");search.setSingleLine(true);search.setTextColor(Color.WHITE);search.setHintTextColor(Color.rgb(160,175,190));search.setBackground(bg(CARD,10));search.setPadding(dp(14),0,dp(14),0);leftPanel.addView(search,new LinearLayout.LayoutParams(-1,dp(48)));
    LinearLayout filters=new LinearLayout(this);filters.setOrientation(LinearLayout.HORIZONTAL);filters.setGravity(Gravity.CENTER_VERTICAL);
    Button allB=button("Todos"),favB=button("Favoritos"),recB=button("Recientes");filters.addView(allB,new LinearLayout.LayoutParams(0,dp(46),1));filters.addView(favB,new LinearLayout.LayoutParams(0,dp(46),1));filters.addView(recB,new LinearLayout.LayoutParams(0,dp(46),1));leftPanel.addView(filters,new LinearLayout.LayoutParams(-1,dp(52)));
    count=label("Cargando canales…",12);count.setTextColor(Color.LTGRAY);leftPanel.addView(count,new LinearLayout.LayoutParams(-1,dp(34)));
    list=new ListView(this);list.setDividerHeight(dp(1));list.setDivider(new android.graphics.drawable.ColorDrawable(Color.rgb(30,55,78)));list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);leftPanel.addView(list,new LinearLayout.LayoutParams(-1,0,1));

    LinearLayout right=new LinearLayout(this);right.setOrientation(LinearLayout.VERTICAL);right.setPadding(dp(10),0,0,0);body.addView(right,new LinearLayout.LayoutParams(0,-1,0.67f));
    title=label("Seleccione un canal",20);title.setTypeface(null,android.graphics.Typeface.BOLD);title.setPadding(dp(8),0,dp(8),0);right.addView(title,new LinearLayout.LayoutParams(-1,dp(48)));
    playerView=new PlayerView(this);playerView.setFocusable(true);playerView.setBackgroundColor(Color.BLACK);right.addView(playerView,new LinearLayout.LayoutParams(-1,0,1));
    status=label("Listo",13);status.setTextColor(Color.LTGRAY);status.setPadding(dp(8),0,dp(8),0);right.addView(status,new LinearLayout.LayoutParams(-1,dp(32)));

    HorizontalScrollView scroll=new HorizontalScrollView(this);scroll.setHorizontalScrollBarEnabled(false);bottomBar=new LinearLayout(this);bottomBar.setOrientation(LinearLayout.HORIZONTAL);bottomBar.setGravity(Gravity.CENTER_VERTICAL);scroll.addView(bottomBar,new HorizontalScrollView.LayoutParams(-2,-1));
    Button prev=button("◀ Anterior"),next=button("Siguiente ▶"),retry=button("↻ Reconectar"),audio=button("🔊 Audio"),subs=button("CC Subtítulos"),pip=button("▣ PiP"),diag=button("Diagnóstico");favButton=button("☆ Favorito");
    for(Button x:new Button[]{prev,favButton,next,retry,audio,subs,pip,diag})bottomBar.addView(x,ctrlLp());right.addView(scroll,new LinearLayout.LayoutParams(-1,dp(58)));
    setContentView(root);

    adapter=new ChannelAdapter();list.setAdapter(adapter);list.setOnItemClickListener((p,v,pos,id)->play(shown.get(pos)));
    allB.setOnClickListener(v->{mode="all";filter();});favB.setOnClickListener(v->{mode="fav";filter();});recB.setOnClickListener(v->{mode="recent";filter();});
    prev.setOnClickListener(v->{if(previous!=null)play(previous);else step(-1);});next.setOnClickListener(v->step(1));retry.setOnClickListener(v->{retries=0;restart();});favButton.setOnClickListener(v->toggleFavorite());
    audio.setOnClickListener(v->showTrackSelector(C.TRACK_TYPE_AUDIO,"Pista de audio"));subs.setOnClickListener(v->showTrackSelector(C.TRACK_TYPE_TEXT,"Subtítulos"));pip.setOnClickListener(v->enterPip());diag.setOnClickListener(v->diagnostics());settings.setOnClickListener(v->settingsDialog());
  }

  void initPlayer(){
    player=new ExoPlayer.Builder(this).build();playerView.setPlayer(player);
    player.addListener(new Player.Listener(){
      @Override public void onPlaybackStateChanged(int s){if(s==Player.STATE_READY){retries=0;status.setText("● En vivo");}else if(s==Player.STATE_BUFFERING)status.setText("Cargando transmisión…");}
      @Override public void onPlayerError(PlaybackException e){autoRetry();}
    });
  }
  void bindSearch(){search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void onTextChanged(CharSequence s,int a,int b,int c){filter();}public void afterTextChanged(android.text.Editable e){}});}

  void loadPrefs(){SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE);favorites.addAll(p.getStringSet("favorites",Collections.emptySet()));String r=p.getString("recents","");if(!r.isEmpty())for(String s:r.split("\\|"))if(!s.isEmpty())recents.add(s);}
  void savePrefs(){getSharedPreferences(PREFS,MODE_PRIVATE).edit().putStringSet("favorites",new HashSet<>(favorites)).putString("recents",joinRecents()).apply();}
  String joinRecents(){StringBuilder b=new StringBuilder();for(String id:recents){if(b.length()>0)b.append("|");b.append(id);}return b.toString();}

  void loadChannels(){
    SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE);String type=p.getString("source_type","official");String val=p.getString("source_value","");count.setText("Cargando lista…");
    io.submit(()->{List<Channel> parsed=null;String src="Lista oficial";
      try{
        if(type.equals("url")&&!val.trim().isEmpty()){parsed=parse(openUrl(val));src="M3U por URL";}
        else if(type.equals("uri")&&!val.trim().isEmpty()){parsed=parse(getContentResolver().openInputStream(Uri.parse(val)));src="M3U local";}
        else {parsed=parse(openUrl(OFFICIAL));src="Lista oficial · GitHub";}
      }catch(Exception ignored){}
      if(parsed==null||parsed.isEmpty()){try{parsed=parse(getAssets().open("TV_Espanol_Plus_VERIFICADA.m3u"));src="Respaldo local · 1,733";}catch(Exception e){parsed=Collections.emptyList();}}
      final List<Channel> r=parsed;final String s=src;runOnUiThread(()->{all.clear();all.addAll(r);filter();sourceLabel.setText(s);count.setText(all.size()+" canales");});
    });
  }
  InputStream openUrl(String u)throws Exception{URLConnection c=new URL(u).openConnection();c.setConnectTimeout(5000);c.setReadTimeout(8000);c.setRequestProperty("User-Agent","TV-Espanol-Plus/1.7");return c.getInputStream();}

  void filter(){if(adapter==null)return;String q=search.getText()==null?"":search.getText().toString().trim().toLowerCase(Locale.ROOT);shown.clear();Set<String> rs=new HashSet<>(recents);for(Channel c:all){boolean m=mode.equals("all")||(mode.equals("fav")&&favorites.contains(c.id))||(mode.equals("recent")&&rs.contains(c.id));if(m&&(q.isEmpty()||c.search.contains(q)))shown.add(c);}if(mode.equals("recent")){final Map<String,Integer> rank=new HashMap<>();int i=0;for(String id:recents)rank.put(id,i++);Collections.sort(shown,new Comparator<Channel>(){public int compare(Channel a,Channel b){Integer ra=rank.get(a.id),rb=rank.get(b.id);return Integer.compare(ra==null?9999:ra,rb==null?9999:rb);}});}adapter.notifyDataSetChanged();count.setText(shown.size()+" de "+all.size()+" canales");}

  void play(Channel c){if(current!=null&&!current.id.equals(c.id))previous=current;current=c;retries=0;title.setText(c.name+"   ·   "+c.group);addRecent(c.id);updateFav();status.setText("Conectando…");player.setMediaItem(new MediaItem.Builder().setUri(c.url).setMediaId(c.id).build());player.prepare();player.play();}
  void restart(){if(current==null)return;status.setText("Reconectando…");player.setMediaItem(new MediaItem.Builder().setUri(current.url).setMediaId(current.id).build());player.prepare();player.play();}
  void autoRetry(){if(current==null)return;if(retries<3){retries++;status.setText("Reconectando "+retries+"/3…");new Handler(getMainLooper()).postDelayed(this::restart,retries*1000L);}else status.setText("Sin señal · use Reconectar o cambie de canal");}
  void step(int d){if(all.isEmpty())return;int i=current==null?-1:all.indexOf(current);i=(i+d+all.size())%all.size();play(all.get(i));}
  void toggleFavorite(){if(current==null)return;if(!favorites.add(current.id))favorites.remove(current.id);savePrefs();updateFav();if(mode.equals("fav"))filter();}
  void updateFav(){favButton.setText(current!=null&&favorites.contains(current.id)?"★ Favorito":"☆ Favorito");}
  void addRecent(String id){recents.remove(id);recents.addFirst(id);while(recents.size()>30)recents.removeLast();savePrefs();}

  @UnstableApi
  void showTrackSelector(int type,String caption){
    if(player==null){toast("Reproductor no disponible");return;}
    try{new TrackSelectionDialogBuilder(this,caption,player,type).setShowDisableOption(true).build().show();}
    catch(Exception e){toast(type==C.TRACK_TYPE_TEXT?"Este canal no ofrece subtítulos seleccionables":"Este canal no ofrece pistas de audio adicionales");}
  }

  @SuppressLint("NewApi")
  void enterPip(){
    if(Build.VERSION.SDK_INT<26){toast("Picture-in-Picture requiere Android 8 o superior");return;}
    if(current==null){toast("Abra un canal antes de usar PiP");return;}
    PictureInPictureParams p=new PictureInPictureParams.Builder().setAspectRatio(new Rational(16,9)).build();enterPictureInPictureMode(p);
  }
  @SuppressLint("NewApi")
  @Override public void onPictureInPictureModeChanged(boolean pip, Configuration cfg){super.onPictureInPictureModeChanged(pip,cfg);leftPanel.setVisibility(pip?View.GONE:View.VISIBLE);topHeader.setVisibility(pip?View.GONE:View.VISIBLE);bottomBar.setVisibility(pip?View.GONE:View.VISIBLE);status.setVisibility(pip?View.GONE:View.VISIBLE);title.setVisibility(pip?View.GONE:View.VISIBLE);}
  @SuppressLint("NewApi")
  @Override protected void onUserLeaveHint(){super.onUserLeaveHint();boolean auto=getSharedPreferences(PREFS,MODE_PRIVATE).getBoolean("auto_pip",false);if(auto&&Build.VERSION.SDK_INT>=26&&current!=null&&player!=null&&player.isPlaying())enterPip();}

  void settingsDialog(){
    SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(22),dp(12),dp(22),dp(6));
    CheckBox auto=new CheckBox(this);auto.setText("Entrar automáticamente en Picture-in-Picture al salir");auto.setChecked(p.getBoolean("auto_pip",false));box.addView(auto,new LinearLayout.LayoutParams(-1,dp(52)));
    Button file=button("📁 Cargar lista M3U desde archivo"),url=button("🌐 Cargar lista M3U por URL"),restore=button("↺ Restaurar lista oficial de TV Español+"),exit=button("Salir de la aplicación");
    for(Button b:new Button[]{file,url,restore,exit}){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(50));lp.setMargins(0,dp(4),0,dp(4));box.addView(b,lp);}
    AlertDialog d=new AlertDialog.Builder(this).setTitle("Configuración · TV Español+ v1.7.0").setView(box).setNegativeButton("Cerrar",null).create();
    auto.setOnCheckedChangeListener((v,checked)->p.edit().putBoolean("auto_pip",checked).apply());file.setOnClickListener(v->{d.dismiss();pickM3u();});url.setOnClickListener(v->{d.dismiss();m3uUrlDialog();});restore.setOnClickListener(v->{p.edit().putString("source_type","official").remove("source_value").apply();d.dismiss();loadChannels();});exit.setOnClickListener(v->{d.dismiss();confirmExit();});d.show();
  }
  void pickM3u(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"audio/x-mpegurl","application/x-mpegURL","application/vnd.apple.mpegurl","text/plain","application/octet-stream"});startActivityForResult(i,REQ_M3U);}
  void m3uUrlDialog(){EditText e=new EditText(this);e.setHint("https://servidor/lista.m3u");e.setSingleLine(true);new AlertDialog.Builder(this).setTitle("Lista M3U por URL").setMessage("Pegue la dirección completa de la lista M3U/M3U8.").setView(e).setNegativeButton("Cancelar",null).setPositiveButton("Cargar",(d,w)->{String u=e.getText().toString().trim();if(u.startsWith("http://")||u.startsWith("https://")){getSharedPreferences(PREFS,MODE_PRIVATE).edit().putString("source_type","url").putString("source_value",u).apply();loadChannels();}else toast("La URL debe comenzar con http:// o https://");}).show();}
  @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);if(requestCode==REQ_M3U&&resultCode==RESULT_OK&&data!=null&&data.getData()!=null){Uri u=data.getData();try{getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}getSharedPreferences(PREFS,MODE_PRIVATE).edit().putString("source_type","uri").putString("source_value",u.toString()).apply();loadChannels();}}

  void diagnostics(){ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);String src=sourceLabel.getText().toString();new AlertDialog.Builder(this).setTitle("Diagnóstico TV Español+").setMessage("Versión: 1.7.0\nCanales cargados: "+all.size()+"\nFuente: "+src+"\nFavoritos: "+favorites.size()+"\nRecientes: "+recents.size()+"\nRed activa: "+(cm.getActiveNetwork()!=null?"Sí":"No")+"\nReproductor: Media3 / ExoPlayer\nPiP: "+(Build.VERSION.SDK_INT>=26?"Compatible":"No disponible en este Android")).setPositiveButton("Cerrar",null).show();}
  void confirmExit(){new AlertDialog.Builder(this).setTitle("Salir de TV Español+").setMessage("¿Desea cerrar la aplicación?").setNegativeButton("Cancelar",null).setPositiveButton("Salir",(d,w)->finishAndRemoveTask()).show();}
  void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}

  @Override public boolean dispatchKeyEvent(KeyEvent e){if(e.getAction()==KeyEvent.ACTION_DOWN){if(e.getKeyCode()==KeyEvent.KEYCODE_MEDIA_NEXT){step(1);return true;}if(e.getKeyCode()==KeyEvent.KEYCODE_MEDIA_PREVIOUS){step(-1);return true;}}return super.dispatchKeyEvent(e);}
  @Override public void onBackPressed(){confirmExit();}
  @Override protected void onDestroy(){super.onDestroy();if(player!=null)player.release();io.shutdownNow();}

  static final Pattern GROUP=Pattern.compile("group-title=\\\"([^\\\"]*)\\\""),ID=Pattern.compile("tvg-id=\\\"([^\\\"]*)\\\"");
  static List<Channel> parse(InputStream in)throws IOException{ArrayList<Channel> out=new ArrayList<>();if(in==null)return out;try(BufferedReader br=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){String line,meta=null;while((line=br.readLine())!=null){line=line.trim();if(line.startsWith("#EXTINF:"))meta=line;else if(!line.isEmpty()&&!line.startsWith("#")&&meta!=null){String name=meta.contains(",")?meta.substring(meta.lastIndexOf(',')+1).trim():"Canal";Matcher gm=GROUP.matcher(meta),im=ID.matcher(meta);String group=gm.find()?gm.group(1):"Otros";String id=im.find()&&im.group(1).trim().length()>0?im.group(1):sha1(name+"|"+line);out.add(new Channel(id,name,group,line));meta=null;}}}return out;}
  static String sha1(String s){try{byte[] b=MessageDigest.getInstance("SHA-1").digest(s.getBytes(StandardCharsets.UTF_8));StringBuilder x=new StringBuilder();for(byte v:b)x.append(String.format("%02x",v));return x.toString();}catch(Exception e){return Integer.toHexString(s.hashCode());}}
  static final class Channel{final String id,name,group,url,search;Channel(String i,String n,String g,String u){id=i;name=n;group=g;url=u;search=(n+" "+g).toLowerCase(Locale.ROOT);}}

  final class ChannelAdapter extends BaseAdapter {
    @Override public int getCount(){return shown.size();}
    @Override public Object getItem(int p){return shown.get(p);}
    @Override public long getItemId(int p){return shown.get(p).id.hashCode();}
    @Override public View getView(int p,View convert,ViewGroup parent){Channel c=shown.get(p);LinearLayout row;if(convert instanceof LinearLayout)row=(LinearLayout)convert;else{row=new LinearLayout(MainActivity.this);row.setOrientation(LinearLayout.VERTICAL);row.setPadding(dp(12),dp(8),dp(12),dp(8));row.setFocusable(true);row.setBackground(bg(Color.rgb(14,31,49),8));TextView n=label("",15);n.setSingleLine(true);TextView g=label("",11);g.setTextColor(Color.LTGRAY);g.setSingleLine(true);row.addView(n,new LinearLayout.LayoutParams(-1,dp(26)));row.addView(g,new LinearLayout.LayoutParams(-1,dp(20)));row.setOnFocusChangeListener((v,has)->v.setBackground(bg(has?Color.rgb(34,78,111):Color.rgb(14,31,49),8)));}((TextView)row.getChildAt(0)).setText(c.name);((TextView)row.getChildAt(1)).setText(c.group);return row;}
  }
}
