package com.epalma.tvespanolplus;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.net.ConnectivityManager;
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
  final ExecutorService io=Executors.newSingleThreadExecutor();
  final List<Channel> all=new ArrayList<>(), shown=new ArrayList<>();
  final LinkedHashSet<String> favorites=new LinkedHashSet<>();
  final ArrayDeque<String> recents=new ArrayDeque<>();
  ArrayAdapter<String> adapter; ExoPlayer player; Channel current, previous;
  ListView list; EditText search; TextView title,status,count; Button favButton; String mode="all"; int retries=0;

  @Override public void onCreate(Bundle b){super.onCreate(b); buildUi(); loadPrefs(); initPlayer(); bind(); loadChannels();}

  TextView text(String s,int sp){TextView v=new TextView(this);v.setText(s);v.setTextColor(Color.WHITE);v.setTextSize(sp);v.setPadding(10,6,10,6);return v;}
  Button button(String s){Button b=new Button(this);b.setText(s);b.setFocusable(true);return b;}
  void buildUi(){
    LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.HORIZONTAL);root.setPadding(12,12,12,12);root.setBackgroundColor(Color.rgb(7,17,31));
    LinearLayout left=new LinearLayout(this);left.setOrientation(LinearLayout.VERTICAL);root.addView(left,new LinearLayout.LayoutParams(dp(360),-1));
    left.addView(text("TV Español+",26),new LinearLayout.LayoutParams(-1,dp(50)));
    search=new EditText(this);search.setHint("Buscar canal, país o categoría…");search.setSingleLine(true);search.setTextColor(Color.WHITE);search.setHintTextColor(Color.LTGRAY);left.addView(search,new LinearLayout.LayoutParams(-1,dp(52)));
    LinearLayout filters=new LinearLayout(this);filters.setOrientation(LinearLayout.HORIZONTAL);Button allB=button("Todos"), favB=button("Favoritos"), recB=button("Recientes");filters.addView(allB,w1());filters.addView(favB,w1());filters.addView(recB,w1());left.addView(filters,new LinearLayout.LayoutParams(-1,dp(54)));
    count=text("Cargando…",13);left.addView(count,new LinearLayout.LayoutParams(-1,dp(38)));
    list=new ListView(this);list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);list.setDividerHeight(2);left.addView(list,new LinearLayout.LayoutParams(-1,0,1));
    LinearLayout right=new LinearLayout(this);right.setOrientation(LinearLayout.VERTICAL);right.setPadding(12,0,0,0);root.addView(right,new LinearLayout.LayoutParams(0,-1,1));
    title=text("Seleccione un canal",22);right.addView(title,new LinearLayout.LayoutParams(-1,dp(50)));
    PlayerView pv=new PlayerView(this);pv.setFocusable(true);right.addView(pv,new LinearLayout.LayoutParams(-1,0,1));
    status=text("Listo",14);right.addView(status,new LinearLayout.LayoutParams(-1,dp(38)));
    LinearLayout controls=new LinearLayout(this);controls.setOrientation(LinearLayout.HORIZONTAL);Button prev=button("◀ Anterior"), next=button("Siguiente ▶"), retry=button("↻ Reconectar"), diag=button("Diagnóstico"), exit=button("Salir");favButton=button("☆ Favorito");for(Button x:new Button[]{prev,favButton,next,retry,diag,exit})controls.addView(x,w1());right.addView(controls,new LinearLayout.LayoutParams(-1,dp(58)));
    setContentView(root); player=new ExoPlayer.Builder(this).build();pv.setPlayer(player);
    allB.setOnClickListener(v->{mode="all";filter();});favB.setOnClickListener(v->{mode="fav";filter();});recB.setOnClickListener(v->{mode="recent";filter();});prev.setOnClickListener(v->{if(previous!=null)play(previous);else step(-1);});next.setOnClickListener(v->step(1));retry.setOnClickListener(v->{retries=0;restart();});diag.setOnClickListener(v->diagnostics());exit.setOnClickListener(v->confirmExit());favButton.setOnClickListener(v->toggleFavorite());
  }
  LinearLayout.LayoutParams w1(){return new LinearLayout.LayoutParams(0,-1,1);} int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
  void initPlayer(){player.addListener(new Player.Listener(){@Override public void onPlaybackStateChanged(int s){if(s==Player.STATE_READY){retries=0;status.setText("Reproduciendo");}else if(s==Player.STATE_BUFFERING)status.setText("Cargando…");}@Override public void onPlayerError(PlaybackException e){autoRetry();}});}
  void bind(){adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,new ArrayList<>());list.setAdapter(adapter);list.setOnItemClickListener((p,v,pos,id)->play(shown.get(pos)));search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void onTextChanged(CharSequence s,int a,int b,int c){filter();}public void afterTextChanged(android.text.Editable e){}});}
  void loadChannels(){count.setText("Cargando lista…");io.submit(()->{List<Channel> parsed=null;String src="GitHub";try{URLConnection c=new URL(REMOTE).openConnection();c.setConnectTimeout(3500);c.setReadTimeout(6000);c.setRequestProperty("User-Agent","TV-Espanol-Plus/1.6");parsed=parse(c.getInputStream());}catch(Exception e){src="respaldo local";}if(parsed==null||parsed.size()<100){try{parsed=parse(getAssets().open("TV_Espanol_Plus_VERIFICADA.m3u"));}catch(Exception e){parsed=Collections.emptyList();}}List<Channel> r=parsed;String source=src;runOnUiThread(()->{all.clear();all.addAll(r);filter();count.setText(all.size()+" canales · "+source);});});}
  void filter(){if(adapter==null)return;String q=search.getText().toString().trim().toLowerCase(Locale.ROOT);shown.clear();Set<String> rs=new HashSet<>(recents);for(Channel c:all){boolean m=mode.equals("all")||(mode.equals("fav")&&favorites.contains(c.id))||(mode.equals("recent")&&rs.contains(c.id));if(m&&(q.isEmpty()||c.search.contains(q)))shown.add(c);}if(mode.equals("recent")){Map<String,Integer> rank=new HashMap<>();int i=0;for(String id:recents)rank.put(id,i++);shown.sort(Comparator.comparingInt(c->rank.getOrDefault(c.id,9999)));}List<String> names=new ArrayList<>();for(Channel c:shown)names.add(c.name+"\n"+c.group);adapter.clear();adapter.addAll(names);adapter.notifyDataSetChanged();count.setText(shown.size()+" de "+all.size()+" canales");}
  void play(Channel c){if(current!=null&&!current.id.equals(c.id))previous=current;current=c;retries=0;title.setText(c.name+" · "+c.group);addRecent(c.id);updateFav();status.setText("Conectando…");player.setMediaItem(new MediaItem.Builder().setUri(c.url).setMediaId(c.id).build());player.prepare();player.play();}
  void restart(){if(current==null)return;status.setText("Reconectando…");player.setMediaItem(new MediaItem.Builder().setUri(current.url).setMediaId(current.id).build());player.prepare();player.play();}
  void autoRetry(){if(current==null)return;if(retries<3){retries++;status.setText("Reconectando "+retries+"/3…");new Handler(getMainLooper()).postDelayed(this::restart,retries*1000L);}else status.setText("Canal sin respuesta");}
  void step(int d){if(all.isEmpty())return;int i=current==null?-1:all.indexOf(current);i=(i+d+all.size())%all.size();play(all.get(i));}
  void toggleFavorite(){if(current==null)return;if(!favorites.add(current.id))favorites.remove(current.id);savePrefs();updateFav();if(mode.equals("fav"))filter();}
  void updateFav(){favButton.setText(current!=null&&favorites.contains(current.id)?"★ Favorito":"☆ Favorito");}
  void addRecent(String id){recents.remove(id);recents.addFirst(id);while(recents.size()>30)recents.removeLast();savePrefs();}
  void loadPrefs(){SharedPreferences p=getSharedPreferences("tvplus",MODE_PRIVATE);favorites.addAll(p.getStringSet("favorites",Collections.emptySet()));String r=p.getString("recents","");if(!r.isEmpty())for(String s:r.split("\\|"))if(!s.isEmpty())recents.add(s);}
  void savePrefs(){getSharedPreferences("tvplus",MODE_PRIVATE).edit().putStringSet("favorites",new HashSet<>(favorites)).putString("recents",String.join("|",recents)).apply();}
  void diagnostics(){ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);new AlertDialog.Builder(this).setTitle("Diagnóstico TV Español+").setMessage("Versión 1.6.0\nCanales: "+all.size()+"\nFavoritos: "+favorites.size()+"\nRecientes: "+recents.size()+"\nRed activa: "+(cm.getActiveNetwork()!=null?"Sí":"No")+"\nReproductor: Media3 / ExoPlayer").setPositiveButton("Cerrar",null).show();}
  void confirmExit(){new AlertDialog.Builder(this).setTitle("Salir de TV Español+").setMessage("¿Desea cerrar la aplicación?").setNegativeButton("Cancelar",null).setPositiveButton("Salir",(d,w)->finishAndRemoveTask()).show();}
  @Override public boolean dispatchKeyEvent(KeyEvent e){if(e.getAction()==KeyEvent.ACTION_DOWN){if(e.getKeyCode()==KeyEvent.KEYCODE_MEDIA_NEXT){step(1);return true;}if(e.getKeyCode()==KeyEvent.KEYCODE_MEDIA_PREVIOUS){step(-1);return true;}}return super.dispatchKeyEvent(e);}
  @Override public void onBackPressed(){confirmExit();}
  @Override protected void onDestroy(){super.onDestroy();if(player!=null)player.release();io.shutdownNow();}

  static final Pattern GROUP=Pattern.compile("group-title=\\\"([^\\\"]*)\\\""),ID=Pattern.compile("tvg-id=\\\"([^\\\"]*)\\\"");
  static List<Channel> parse(InputStream in)throws IOException{ArrayList<Channel> out=new ArrayList<>();try(BufferedReader br=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){String line,meta=null;while((line=br.readLine())!=null){line=line.trim();if(line.startsWith("#EXTINF:"))meta=line;else if(!line.isEmpty()&&!line.startsWith("#")&&meta!=null){String name=meta.contains(",")?meta.substring(meta.lastIndexOf(',')+1).trim():"Canal";Matcher gm=GROUP.matcher(meta),im=ID.matcher(meta);String group=gm.find()?gm.group(1):"Otros";String id=im.find()&&!im.group(1).isBlank()?im.group(1):sha1(name+"|"+line);out.add(new Channel(id,name,group,line));meta=null;}}}return out;}
  static String sha1(String s){try{byte[] b=MessageDigest.getInstance("SHA-1").digest(s.getBytes(StandardCharsets.UTF_8));StringBuilder x=new StringBuilder();for(byte v:b)x.append(String.format("%02x",v));return x.toString();}catch(Exception e){return Integer.toHexString(s.hashCode());}}
  static final class Channel{final String id,name,group,url,search;Channel(String i,String n,String g,String u){id=i;name=n;group=g;url=u;search=(n+" "+g).toLowerCase(Locale.ROOT);}}
}
