package com.epalma.tvespanolplus;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.*;
import android.os.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.media3.common.*;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.media3.ui.AspectRatioFrameLayout;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

@UnstableApi
public class MainActivity extends Activity {
  static final int OPEN_M3U=7001;
  static final int NAVY=Color.rgb(4,12,23), PANEL=Color.rgb(9,28,46), CARD=Color.rgb(17,43,66), ACCENT=Color.rgb(25,155,255), CYAN=Color.rgb(32,221,232), BLUE=Color.rgb(27,105,255), MUTED=Color.rgb(166,188,207), GREEN=Color.rgb(32,189,129), PURPLE=Color.rgb(124,92,255), ORANGE=Color.rgb(255,151,54);
  final ExecutorService io=Executors.newSingleThreadExecutor();
  final List<Channel> all=new ArrayList<>(), shown=new ArrayList<>();
  final LinkedHashSet<String> favorites=new LinkedHashSet<>();
  final ArrayDeque<String> recents=new ArrayDeque<>();
  ExoPlayer player; PlayerView playerView; FrameLayout homePlayerHolder, contentFrame;
  Channel current, previous; ArrayAdapter<String> adapter; ListView channelList; EditText search; TextView status,count,sourceText,title;
  Button favButton; View playerEmpty; String mode="all", category="Todos"; int retries=0, resizeMode=0; boolean autoRetryEnabled=true;

  @Override public void onCreate(Bundle b){super.onCreate(b);loadPrefs();buildShell();initPlayer();showHome();loadSavedSource();}

  int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
  GradientDrawable rounded(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
  StateListDrawable buttonBg(int normal,int pressed,int radius){StateListDrawable s=new StateListDrawable();s.addState(new int[]{android.R.attr.state_pressed},rounded(pressed,radius));s.addState(new int[]{android.R.attr.state_focused},rounded(pressed,radius));s.addState(new int[]{},rounded(normal,radius));return s;}
  TextView tv(String s,int sp,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(color);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
  Button btn(String s){return actionBtn(s,CARD);}
  Button actionBtn(String s,int color){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setTextSize(13);b.setMinHeight(dp(46));b.setFocusable(true);b.setGravity(Gravity.CENTER);b.setBackground(buttonBg(color,blend(color,Color.WHITE,.14f),13));b.setPadding(dp(10),0,dp(10),0);b.setOnFocusChangeListener((v,f)->{v.setScaleX(f?1.035f:1f);v.setScaleY(f?1.035f:1f);});return b;}
  int blend(int a,int b,float f){int r=(int)(Color.red(a)*(1-f)+Color.red(b)*f),g=(int)(Color.green(a)*(1-f)+Color.green(b)*f),bl=(int)(Color.blue(a)*(1-f)+Color.blue(b)*f);return Color.rgb(r,g,bl);}
  LinearLayout.LayoutParams weight(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);p.setMargins(dp(3),0,dp(3),0);return p;}

  void buildShell(){
    getWindow().setStatusBarColor(NAVY);getWindow().setNavigationBarColor(NAVY);
    LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(NAVY);
    root.setOnApplyWindowInsetsListener((v,in)->{v.setPadding(in.getSystemWindowInsetLeft(),in.getSystemWindowInsetTop(),in.getSystemWindowInsetRight(),in.getSystemWindowInsetBottom());return in;});

    LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(dp(12),dp(4),dp(8),dp(4));
    ImageView icon=new ImageView(this);icon.setImageResource(com.epalma.tvespanolplus.R.drawable.palmavision_icon);icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);top.addView(icon,new LinearLayout.LayoutParams(dp(48),dp(48)));
    LinearLayout brand=new LinearLayout(this);brand.setOrientation(LinearLayout.VERTICAL);brand.setPadding(dp(8),0,0,0);
    TextView brandName=tv("PalmaVision",21,Color.WHITE);brandName.setTypeface(Typeface.DEFAULT,Typeface.BOLD);brand.addView(brandName,new LinearLayout.LayoutParams(-1,dp(29)));
    TextView sub=tv("TV en vivo · películas · series",11,CYAN);brand.addView(sub,new LinearLayout.LayoutParams(-1,dp(18)));top.addView(brand,new LinearLayout.LayoutParams(0,dp(52),1));
    sourceText=tv("Lista principal",11,MUTED);sourceText.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);top.addView(sourceText,new LinearLayout.LayoutParams(dp(116),dp(48)));
    Button gear=actionBtn("⚙",Color.rgb(22,63,92));gear.setContentDescription("Configuración");gear.setTextSize(19);top.addView(gear,new LinearLayout.LayoutParams(dp(52),dp(46)));gear.setOnClickListener(v->showSettings());
    root.addView(top,new LinearLayout.LayoutParams(-1,dp(60)));

    contentFrame=new FrameLayout(this);root.addView(contentFrame,new LinearLayout.LayoutParams(-1,0,1));

    LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);nav.setPadding(dp(8),dp(4),dp(8),dp(4));
    Button nTv=actionBtn("📺  TV",Color.rgb(19,73,108)), nFav=actionBtn("★  Favoritos",Color.rgb(39,69,108)), nRecent=actionBtn("↻  Recientes",Color.rgb(30,68,91)), nCat=actionBtn("▦  Categorías",Color.rgb(20,86,108));
    for(Button btt:new Button[]{nTv,nFav,nRecent,nCat})nav.addView(btt,weight());
    nTv.setOnClickListener(v->showHome());nFav.setOnClickListener(v->showSavedPage("Favoritos",true));nRecent.setOnClickListener(v->showSavedPage("Recientes",false));nCat.setOnClickListener(v->showCategories());
    root.addView(nav,new LinearLayout.LayoutParams(-1,dp(58)));
    setContentView(root);
  }

  void clearContent(){contentFrame.removeAllViews();}
  void showHome(){
    clearContent();mode="all";category="Todos";
    LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(10),dp(4),dp(10),dp(8));contentFrame.addView(page,new FrameLayout.LayoutParams(-1,-1));
    search=new EditText(this);search.setSingleLine(true);search.setHint("Buscar canal, país o categoría…");search.setTextColor(Color.WHITE);search.setHintTextColor(MUTED);search.setTextSize(16);search.setBackground(rounded(PANEL,14));search.setPadding(dp(14),0,dp(14),0);page.addView(search,new LinearLayout.LayoutParams(-1,dp(48)));
    LinearLayout catHead=new LinearLayout(this);catHead.setGravity(Gravity.CENTER_VERTICAL);TextView catHint=tv("Explorar categorías",12,MUTED);catHead.addView(catHint,new LinearLayout.LayoutParams(0,dp(24),1));TextView swipe=tv("Desliza  →",11,CYAN);swipe.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);catHead.addView(swipe,new LinearLayout.LayoutParams(dp(92),dp(24)));page.addView(catHead,new LinearLayout.LayoutParams(-1,dp(24)));
    HorizontalScrollView hsv=new HorizontalScrollView(this);hsv.setHorizontalScrollBarEnabled(false);hsv.setOverScrollMode(View.OVER_SCROLL_NEVER);LinearLayout chips=new LinearLayout(this);chips.setPadding(0,dp(4),dp(28),dp(7));hsv.addView(chips);
    String[] cats={"Todos","⚽ Deportes","🎬 Películas","📺 Series","📰 Noticias","🙏 Religión","👧 Infantil","🎵 Música","🧭 Más categorías  ›"};
    int[] catColors={BLUE,Color.rgb(30,137,89),PURPLE,Color.rgb(29,112,176),Color.rgb(166,64,77),Color.rgb(73,91,190),ORANGE,Color.rgb(167,62,153),Color.rgb(20,105,124)};
    for(int i=0;i<cats.length;i++){final String raw=cats[i];final String c=raw.contains(" ")?raw.substring(raw.indexOf(' ')+1).replace(" categorías  ›","").replace("Infantil","Niños"):raw;Button b=actionBtn(raw,catColors[i]);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,dp(40));p.setMargins(0,0,dp(7),0);chips.addView(b,p);b.setOnClickListener(v->{if(raw.contains("Más categorías")){showCategories();return;}category=c;filter();});}
    page.addView(hsv,new LinearLayout.LayoutParams(-1,dp(51)));

    LinearLayout body=new LinearLayout(this);boolean wide=getResources().getConfiguration().screenWidthDp>=700;body.setOrientation(wide?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);page.addView(body,new LinearLayout.LayoutParams(-1,0,1));
    LinearLayout playerCard=buildPlayerCard();LinearLayout listCard=buildChannelList();
    if(wide){body.addView(listCard,new LinearLayout.LayoutParams(dp(335),-1));LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,-1,1);rp.setMargins(dp(10),0,0,0);body.addView(playerCard,rp);}else{body.addView(playerCard,new LinearLayout.LayoutParams(-1,dp(390)));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,0,1);lp.setMargins(0,dp(8),0,0);body.addView(listCard,lp);}
    bindList();filter();
  }

  LinearLayout buildPlayerCard(){
    LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setBackground(rounded(PANEL,14));card.setPadding(dp(6),dp(6),dp(6),dp(6));
    title=tv(current==null?"Selecciona un canal para comenzar":current.name,18,Color.WHITE);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setPadding(dp(8),0,dp(8),0);card.addView(title,new LinearLayout.LayoutParams(-1,dp(42)));
    homePlayerHolder=new FrameLayout(this);homePlayerHolder.setBackgroundColor(Color.BLACK);card.addView(homePlayerHolder,new LinearLayout.LayoutParams(-1,0,1));
    attachMainPlayer();
    if(current==null){LinearLayout empty=new LinearLayout(this);empty.setOrientation(LinearLayout.VERTICAL);empty.setGravity(Gravity.CENTER);ImageView mark=new ImageView(this);mark.setImageResource(com.epalma.tvespanolplus.R.drawable.palmavision_icon);mark.setAlpha(.82f);empty.addView(mark,new LinearLayout.LayoutParams(dp(84),dp(84)));TextView msg=tv("PalmaVision\nElige un canal, película o serie",14,Color.WHITE);msg.setGravity(Gravity.CENTER);msg.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);empty.addView(msg,new LinearLayout.LayoutParams(-1,dp(54)));playerEmpty=empty;homePlayerHolder.addView(empty,new FrameLayout.LayoutParams(-1,-1));}
    status=tv(current==null?"Listo para reproducir":"● En vivo",12,current==null?MUTED:CYAN);status.setPadding(dp(8),0,dp(8),0);card.addView(status,new LinearLayout.LayoutParams(-1,dp(30)));
    LinearLayout controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER);Button prev=actionBtn("⏮\nAnterior",Color.rgb(28,65,95)),next=actionBtn("⏭\nSiguiente",Color.rgb(28,65,95)),retry=actionBtn("↻\nReconectar",Color.rgb(35,79,102)),more=actionBtn("•••\nMás",Color.rgb(54,64,112));favButton=actionBtn("☆\nFavorito",Color.rgb(96,69,44));
    for(Button b:new Button[]{prev,favButton,next,retry,more}){b.setTextSize(11);b.setGravity(Gravity.CENTER);controls.addView(b,weight());}card.addView(controls,new LinearLayout.LayoutParams(-1,dp(58)));
    LinearLayout videoTools=new LinearLayout(this);videoTools.setGravity(Gravity.CENTER);Button full=actionBtn("⛶  Pantalla completa",BLUE),fit=actionBtn("▣  "+resizeModeName(),Color.rgb(23,88,106));videoTools.addView(full,weight());videoTools.addView(fit,weight());card.addView(videoTools,new LinearLayout.LayoutParams(-1,dp(50)));
    prev.setOnClickListener(v->{if(previous!=null)play(previous);else step(-1);});next.setOnClickListener(v->step(1));retry.setOnClickListener(v->{retries=0;restart();});favButton.setOnClickListener(v->toggleFavorite());more.setOnClickListener(v->showMoreMenu());full.setOnClickListener(v->openFullscreen());fit.setOnClickListener(v->{cycleResizeMode();fit.setText("▣  "+resizeModeName());});updateFav();return card;
  }

  LinearLayout buildChannelList(){
    LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setBackground(rounded(PANEL,14));
    count=tv("Cargando…",12,MUTED);count.setPadding(dp(12),0,dp(8),0);card.addView(count,new LinearLayout.LayoutParams(-1,dp(34)));
    channelList=new ListView(this);channelList.setDivider(new ColorDrawable(Color.rgb(27,48,67)));channelList.setDividerHeight(1);card.addView(channelList,new LinearLayout.LayoutParams(-1,0,1));return card;
  }

  void attachMainPlayer(){if(playerView==null){playerView=new PlayerView(this);playerView.setUseController(true);playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING);playerView.setFocusable(true);}applyResizeMode(playerView);ViewParent p=playerView.getParent();if(p instanceof ViewGroup)((ViewGroup)p).removeView(playerView);if(homePlayerHolder!=null){homePlayerHolder.addView(playerView,0,new FrameLayout.LayoutParams(-1,-1));if(player!=null)playerView.setPlayer(player);}}

  void initPlayer(){player=new ExoPlayer.Builder(this).build();player.addListener(new Player.Listener(){@Override public void onPlaybackStateChanged(int s){if(status==null)return;if(s==Player.STATE_READY){retries=0;status.setText("● En vivo");status.setTextColor(CYAN);}else if(s==Player.STATE_BUFFERING)status.setText("Cargando…");}@Override public void onPlayerError(PlaybackException e){if(autoRetryEnabled)autoRetry();else if(status!=null)status.setText("Canal sin respuesta");}});attachMainPlayer();}

  void bindList(){adapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,new ArrayList<String>()){@Override public View getView(int p,View c,ViewGroup parent){TextView v=(TextView)super.getView(p,c,parent);v.setTextColor(Color.WHITE);v.setTextSize(14);v.setPadding(dp(12),dp(8),dp(8),dp(8));v.setMinHeight(dp(62));v.setBackgroundColor(p%2==0?Color.rgb(11,31,49):Color.rgb(13,35,54));return v;}};channelList.setAdapter(adapter);channelList.setOnItemClickListener((p,v,pos,id)->play(shown.get(pos)));search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void onTextChanged(CharSequence s,int a,int b,int c){filter();}public void afterTextChanged(android.text.Editable e){}});}

  boolean categoryMatch(Channel c,String cat){if(cat.equals("Todos"))return true;String x=(c.name+" "+c.group).toLowerCase(Locale.ROOT);if(cat.equals("Deportes"))return has(x,"sport","deport","futbol","fútbol","soccer","basket","tenis","golf","poker");if(cat.equals("Películas"))return has(x,"movie","pelicula","película","cine","cinema","film");if(cat.equals("Series"))return has(x,"serie","series","tv show","telenovela","novela","drama");if(cat.equals("Religión"))return has(x,"relig","crist","iglesia","church","catolic","católic","evangel","enlace","ewtn");if(cat.equals("Noticias"))return has(x,"noticia","news","24h","informativo");if(cat.equals("Niños"))return has(x,"niño","nino","kids","child","cartoon","infantil");if(cat.equals("Música"))return has(x,"music","música","musica","radio");if(cat.equals("Documentales"))return has(x,"document","history","historia","science","ciencia","nature","naturaleza");return true;}
  boolean has(String x,String...keys){for(String k:keys)if(x.contains(k))return true;return false;}

  void filter(){if(adapter==null||search==null)return;String q=search.getText().toString().trim().toLowerCase(Locale.ROOT);shown.clear();Set<String> recentSet=new HashSet<>(recents);for(Channel c:all){boolean mm=mode.equals("all")||(mode.equals("fav")&&favorites.contains(c.id))||(mode.equals("recent")&&recentSet.contains(c.id));if(mm&&categoryMatch(c,category)&&(q.isEmpty()||c.search.contains(q)))shown.add(c);}if(mode.equals("recent")){final Map<String,Integer> rank=new HashMap<>();int i=0;for(String id:recents)rank.put(id,i++);Collections.sort(shown,(a,b)->Integer.compare(rank.containsKey(a.id)?rank.get(a.id):9999,rank.containsKey(b.id)?rank.get(b.id):9999));}List<String> names=new ArrayList<>();for(Channel c:shown)names.add((favorites.contains(c.id)?"★  ":"📺  ")+c.name+"\n     "+c.group);adapter.clear();adapter.addAll(names);adapter.notifyDataSetChanged();count.setText(shown.size()+" de "+all.size()+" canales · "+category);}

  void play(Channel c){if(current!=null&&!current.id.equals(c.id))previous=current;current=c;retries=0;addRecent(c.id);if(title!=null)title.setText(c.name+"  ·  "+c.group);if(status!=null){status.setText("Conectando…");status.setTextColor(CYAN);}if(playerEmpty!=null)playerEmpty.setVisibility(View.GONE);updateFav();player.setMediaItem(new MediaItem.Builder().setUri(c.url).setMediaId(c.id).build());player.prepare();player.play();}
  void restart(){if(current==null)return;if(status!=null)status.setText("Reconectando…");player.setMediaItem(new MediaItem.Builder().setUri(current.url).setMediaId(current.id).build());player.prepare();player.play();}
  void autoRetry(){if(current==null)return;if(retries<3){retries++;if(status!=null)status.setText("Reconectando "+retries+"/3…");new Handler(getMainLooper()).postDelayed(this::restart,retries*900L);}else if(status!=null)status.setText("Canal sin respuesta");}
  void step(int d){if(all.isEmpty())return;int i=current==null?-1:all.indexOf(current);i=(i+d+all.size())%all.size();play(all.get(i));}
  void toggleFavorite(){if(current==null)return;if(!favorites.add(current.id))favorites.remove(current.id);savePrefs();updateFav();}
  void updateFav(){if(favButton!=null)favButton.setText(current!=null&&favorites.contains(current.id)?"★\nGuardado":"☆\nFavorito");}
  void addRecent(String id){recents.remove(id);recents.addFirst(id);while(recents.size()>30)recents.removeLast();savePrefs();}

  void showMoreMenu(){clearContent();LinearLayout page=pageBase("Más opciones","Herramientas esenciales de PalmaVision CORE");Button full=menuCard("⛶  Pantalla completa","Aprovecha toda la pantalla",BLUE),fit=menuCard("▣  Ajuste de imagen · "+resizeModeName(),"Ajustar, llenar, estirar u original",Color.rgb(31,105,124)),audio=menuCard("🔊  Audio","Selecciona la pista de audio",Color.rgb(23,111,146)),subs=menuCard("CC  Subtítulos","Activa o cambia subtítulos",Color.rgb(91,80,151)),lists=menuCard("☰  Listas M3U","Lista principal, archivo local o URL",Color.rgb(33,96,88)),settings=menuCard("⚙  Configuración","Preferencias y diagnóstico",Color.rgb(43,82,111)),exit=menuCard("⏻  Salir","Cerrar PalmaVision",Color.rgb(111,53,61));for(Button b:new Button[]{full,fit,audio,subs,lists,settings,exit}){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(62));p.setMargins(0,dp(5),0,0);page.addView(b,p);}full.setOnClickListener(v->openFullscreen());fit.setOnClickListener(v->{cycleResizeMode();showMoreMenu();});audio.setOnClickListener(v->showTracks(C.TRACK_TYPE_AUDIO));subs.setOnClickListener(v->showTracks(C.TRACK_TYPE_TEXT));lists.setOnClickListener(v->showLists());settings.setOnClickListener(v->showSettings());exit.setOnClickListener(v->confirmExit());}

  void showTracks(int type){if(current==null){toast("Seleccione un canal primero");return;}Tracks t=player.getCurrentTracks();final List<TrackSelectionOverride> ovs=new ArrayList<>();final List<String> labels=new ArrayList<>();if(type==C.TRACK_TYPE_TEXT)labels.add("Desactivados");for(Tracks.Group g:t.getGroups()){if(g.getType()!=type)continue;for(int i=0;i<g.length;i++){Format f=g.getTrackFormat(i);String l=f.label!=null?f.label:(f.language!=null?f.language:(type==C.TRACK_TYPE_AUDIO?"Audio ":"Subtítulo ")+(labels.size()+1));labels.add(l);ovs.add(new TrackSelectionOverride(g.getMediaTrackGroup(),i));}}if(labels.isEmpty()||(type==C.TRACK_TYPE_TEXT&&labels.size()==1)){toast(type==C.TRACK_TYPE_AUDIO?"No hay pistas de audio adicionales":"Este canal no ofrece subtítulos");return;}new AlertDialog.Builder(this).setTitle(type==C.TRACK_TYPE_AUDIO?"Audio":"Subtítulos").setItems(labels.toArray(new String[0]),(d,w)->{TrackSelectionParameters.Builder b=player.getTrackSelectionParameters().buildUpon();if(type==C.TRACK_TYPE_TEXT&&w==0)b.setTrackTypeDisabled(C.TRACK_TYPE_TEXT,true);else{int x=type==C.TRACK_TYPE_TEXT?w-1:w;b.setTrackTypeDisabled(type,false);b.setOverrideForType(ovs.get(x));}player.setTrackSelectionParameters(b.build());}).show();}

  void showSavedPage(String label,boolean fav){clearContent();LinearLayout page=pageBase(label,fav?"Tus canales guardados":"Últimos canales vistos");List<Channel> data=new ArrayList<>();if(fav){for(Channel c:all)if(favorites.contains(c.id))data.add(c);}else{for(String id:recents)for(Channel c:all)if(c.id.equals(id)){data.add(c);break;}}addChannelRows(page,data);}
  void showCategories(){clearContent();LinearLayout page=pageBase("Categorías","Todo el contenido, organizado para encontrarlo rápido");String[] cats={"⚽ Deportes","🎬 Películas","📺 Series","📰 Noticias","🙏 Religión","👧 Infantil","🎵 Música","🌎 Documentales"};int[] colors={Color.rgb(28,132,85),PURPLE,Color.rgb(28,105,169),Color.rgb(158,62,75),Color.rgb(69,88,184),ORANGE,Color.rgb(161,58,146),Color.rgb(32,112,126)};GridLayout grid=new GridLayout(this);grid.setColumnCount(getResources().getConfiguration().screenWidthDp>=600?4:2);page.addView(grid,new LinearLayout.LayoutParams(-1,-2));for(int i=0;i<cats.length;i++){final String raw=cats[i];final String c=raw.substring(raw.indexOf(' ')+1).replace("Infantil","Niños");int n=0;for(Channel ch:all)if(categoryMatch(ch,c))n++;Button b=actionBtn(raw+"\n"+n+" canales",colors[i]);b.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);b.setTextSize(13);GridLayout.LayoutParams gp=new GridLayout.LayoutParams();gp.width=0;gp.height=dp(86);gp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);gp.setMargins(dp(4),dp(4),dp(4),dp(4));grid.addView(b,gp);b.setOnClickListener(v->showCategoryChannels(c));}}
  void showCategoryChannels(String cat){clearContent();LinearLayout page=pageBase(cat,"Canales agrupados automáticamente");ArrayList<Channel> data=new ArrayList<>();for(Channel c:all)if(categoryMatch(c,cat))data.add(c);addChannelRows(page,data);}
  LinearLayout pageBase(String head,String sub){LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(14),dp(8),dp(14),dp(10));contentFrame.addView(page,new FrameLayout.LayoutParams(-1,-1));LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);ImageView mark=new ImageView(this);mark.setImageResource(com.epalma.tvespanolplus.R.drawable.palmavision_icon);row.addView(mark,new LinearLayout.LayoutParams(dp(38),dp(38)));LinearLayout texts=new LinearLayout(this);texts.setOrientation(LinearLayout.VERTICAL);texts.setPadding(dp(10),0,0,0);TextView h=tv(head,22,Color.WHITE);h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);texts.addView(h,new LinearLayout.LayoutParams(-1,dp(28)));TextView ss=tv(sub,12,CYAN);texts.addView(ss,new LinearLayout.LayoutParams(-1,dp(22)));row.addView(texts,new LinearLayout.LayoutParams(0,dp(52),1));page.addView(row,new LinearLayout.LayoutParams(-1,dp(56)));return page;}
  Button menuCard(String title,String sub,int color){Button b=actionBtn(title+"\n"+sub,color);b.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);b.setTextSize(13);b.setPadding(dp(16),0,dp(12),0);return b;}
  void addChannelRows(LinearLayout page,List<Channel> data){ListView lv=new ListView(this);ArrayList<String> labels=new ArrayList<>();for(Channel c:data)labels.add(c.name+"\n"+c.group);ArrayAdapter<String>a=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,labels){@Override public View getView(int p,View c,ViewGroup parent){TextView v=(TextView)super.getView(p,c,parent);v.setTextColor(Color.WHITE);v.setTextSize(15);v.setPadding(dp(12),dp(8),dp(8),dp(8));return v;}};lv.setAdapter(a);lv.setDivider(new ColorDrawable(Color.rgb(27,48,67)));lv.setDividerHeight(1);page.addView(lv,new LinearLayout.LayoutParams(-1,0,1));lv.setOnItemClickListener((p,v,pos,id)->{Channel c=data.get(pos);showHome();play(c);});if(data.isEmpty()){TextView empty=tv("No hay canales en esta sección.",14,MUTED);page.addView(empty,new LinearLayout.LayoutParams(-1,dp(50)));}}

  void showSettings(){clearContent();LinearLayout page=pageBase("Configuración","PalmaVision v1.9.6 CORE · estabilidad primero");
    Switch retry=new Switch(this);retry.setText("  ↻  Reconexión automática");retry.setTextColor(Color.WHITE);retry.setChecked(autoRetryEnabled);retry.setBackground(rounded(Color.rgb(18,55,78),14));retry.setPadding(dp(12),0,dp(12),0);page.addView(retry,new LinearLayout.LayoutParams(-1,dp(60)));
    Button lists=menuCard("☰  Listas M3U","Administra tus fuentes",Color.rgb(31,97,91)), audio=menuCard("🔊  Audio","Pistas disponibles del canal",Color.rgb(24,104,143)), subs=menuCard("CC  Subtítulos","Activa o cambia subtítulos",Color.rgb(81,74,148)),screen=menuCard("⛶  Pantalla y formato","Pantalla completa · "+resizeModeName(),BLUE),diag=menuCard("✓  Diagnóstico y seguridad","Estado de red y permisos",Color.rgb(34,112,91)),about=menuCard("ⓘ  Acerca de PalmaVision","Versión CORE y plataforma",Color.rgb(51,77,105));
    for(Button b:new Button[]{lists,audio,subs,screen,diag,about}){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(62));p.setMargins(0,dp(5),0,0);page.addView(b,p);}retry.setOnCheckedChangeListener((v,on)->{autoRetryEnabled=on;savePrefs();});lists.setOnClickListener(v->showLists());audio.setOnClickListener(v->showTracks(C.TRACK_TYPE_AUDIO));subs.setOnClickListener(v->showTracks(C.TRACK_TYPE_TEXT));screen.setOnClickListener(v->showMoreMenu());diag.setOnClickListener(v->diagnostics());about.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("PalmaVision").setMessage("Versión 1.9.6 CORE\nMedia3 / ExoPlayer\nAndroid / Android TV\n\nFase 1 · estabilidad, M3U y reproducción").setPositiveButton("Cerrar",null).show());}

  void showLists(){clearContent();LinearLayout page=pageBase("Mis listas","Elegí la fuente sin perder favoritos ni recientes");Button principal=btn("✓  PalmaVision · lista principal (1,733)"),local=btn("▣  Abrir archivo M3U local"),url=btn("↗  Cargar lista desde URL");for(Button b:new Button[]{principal,local,url}){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(58));p.setMargins(0,dp(6),0,0);page.addView(b,p);}principal.setOnClickListener(v->{getSharedPreferences("tvplus",MODE_PRIVATE).edit().remove("m3u_uri").remove("m3u_url").apply();loadDefault();showHome();});local.setOnClickListener(v->pickM3u());url.setOnClickListener(v->askUrl());}
  void pickM3u(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");startActivityForResult(i,OPEN_M3U);}
  @Override protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(req==OPEN_M3U&&res==RESULT_OK&&data!=null&&data.getData()!=null){Uri u=data.getData();try{getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}getSharedPreferences("tvplus",MODE_PRIVATE).edit().putString("m3u_uri",u.toString()).remove("m3u_url").apply();loadFromUri(u,"M3U local");showHome();}}
  void askUrl(){final EditText e=new EditText(this);e.setHint("https://servidor/lista.m3u");e.setSingleLine(true);new AlertDialog.Builder(this).setTitle("URL de lista M3U").setView(e).setNegativeButton("Cancelar",null).setPositiveButton("Cargar",(d,w)->{String u=e.getText().toString().trim();if(u.startsWith("http://")||u.startsWith("https://")){getSharedPreferences("tvplus",MODE_PRIVATE).edit().putString("m3u_url",u).remove("m3u_uri").apply();loadFromUrl(u,"M3U URL");showHome();}else toast("Ingrese una URL válida");}).show();}

  String resizeModeName(){switch(resizeMode){case 1:return "Llenar";case 2:return "Estirar";case 3:return "Original";default:return "Ajustar";}}
  void applyResizeMode(PlayerView v){if(v==null)return;int m=AspectRatioFrameLayout.RESIZE_MODE_FIT;if(resizeMode==1)m=AspectRatioFrameLayout.RESIZE_MODE_ZOOM;else if(resizeMode==2)m=AspectRatioFrameLayout.RESIZE_MODE_FILL;else if(resizeMode==3)m=getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE?AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT:AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH;v.setResizeMode(m);}
  void cycleResizeMode(){resizeMode=(resizeMode+1)%4;applyResizeMode(playerView);savePrefs();toast("Formato: "+resizeModeName());}
  void openFullscreen(){if(current==null){toast("Selecciona un canal primero");return;}final Dialog dlg=new Dialog(this,android.R.style.Theme_Material_NoActionBar_Fullscreen);FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);ViewParent old=playerView.getParent();if(old instanceof ViewGroup)((ViewGroup)old).removeView(playerView);root.addView(playerView,new FrameLayout.LayoutParams(-1,-1));applyResizeMode(playerView);LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(10),dp(6),dp(10),dp(6));bar.setBackgroundColor(Color.argb(190,4,12,23));TextView name=tv(current.name,14,Color.WHITE);name.setTypeface(Typeface.DEFAULT,Typeface.BOLD);bar.addView(name,new LinearLayout.LayoutParams(0,dp(46),1));Button fit=actionBtn("▣ "+resizeModeName(),Color.rgb(21,89,108));Button close=actionBtn("✕ Cerrar",Color.rgb(80,44,60));bar.addView(fit,new LinearLayout.LayoutParams(dp(118),dp(44)));bar.addView(close,new LinearLayout.LayoutParams(dp(104),dp(44)));FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(-1,dp(58),Gravity.BOTTOM);root.addView(bar,bp);fit.setOnClickListener(v->{cycleResizeMode();fit.setText("▣ "+resizeModeName());});close.setOnClickListener(v->dlg.dismiss());dlg.setOnDismissListener(d->{attachMainPlayer();if(player!=null)player.setVolume(1f);});dlg.setContentView(root);dlg.show();Window w=dlg.getWindow();if(w!=null){w.setStatusBarColor(Color.BLACK);w.setNavigationBarColor(Color.BLACK);w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}}

  void diagnostics(){ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);new AlertDialog.Builder(this).setTitle("Diagnóstico PalmaVision").setMessage("Versión: 1.9.6 CORE\nCanales: "+all.size()+"\nFavoritos: "+favorites.size()+"\nRecientes: "+recents.size()+"\nRed activa: "+(cm.getActiveNetwork()!=null?"Sí":"No")+"\nPermisos: INTERNET + ACCESS_NETWORK_STATE\nBuild: release / no-debuggable\nReproductor: Media3 / ExoPlayer").setPositiveButton("Cerrar",null).show();}
  void confirmExit(){new AlertDialog.Builder(this).setTitle("Salir de PalmaVision").setMessage("¿Desea cerrar la aplicación?").setNegativeButton("Cancelar",null).setPositiveButton("Salir",(d,w)->finishAndRemoveTask()).show();}
  void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}

  void loadSavedSource(){SharedPreferences p=getSharedPreferences("tvplus",MODE_PRIVATE);String uri=p.getString("m3u_uri",""),url=p.getString("m3u_url","");if(!uri.isEmpty()){loadFromUri(Uri.parse(uri),"M3U local");return;}if(!url.isEmpty()){loadFromUrl(url,"M3U URL");return;}loadDefault();}
  void loadDefault(){setLoading("Cargando lista…","Lista principal");io.submit(()->{List<Channel> parsed;try{parsed=parse(getAssets().open("TV_Espanol_Plus_VERIFICADA.m3u"));}catch(Exception e){parsed=Collections.emptyList();}applyChannels(parsed,"Lista principal");});}
  void loadFromUrl(String url,String label){setLoading("Cargando M3U…",label);io.submit(()->{try{URLConnection c=new URL(url).openConnection();c.setConnectTimeout(5000);c.setReadTimeout(9000);c.setRequestProperty("User-Agent","PalmaVision/1.9.6");List<Channel> p=parse(c.getInputStream());if(p.isEmpty())throw new IOException("Lista vacía");applyChannels(p,label);}catch(Exception e){runOnUiThread(()->toast("No se pudo cargar la lista: "+e.getMessage()));}});}
  void loadFromUri(Uri uri,String label){setLoading("Leyendo archivo M3U…",label);io.submit(()->{try(InputStream in=getContentResolver().openInputStream(uri)){List<Channel> p=parse(in);if(p.isEmpty())throw new IOException("Lista vacía");applyChannels(p,label);}catch(Exception e){runOnUiThread(()->toast("No se pudo leer M3U: "+e.getMessage()));}});}
  void setLoading(String s,String src){runOnUiThread(()->{if(count!=null)count.setText(s);if(sourceText!=null)sourceText.setText(src);});}
  void applyChannels(List<Channel> p,String label){runOnUiThread(()->{all.clear();all.addAll(p);if(sourceText!=null)sourceText.setText(label);if(adapter!=null)filter();});}

  void loadPrefs(){SharedPreferences p=getSharedPreferences("tvplus",MODE_PRIVATE);favorites.addAll(p.getStringSet("favorites",Collections.emptySet()));String r=p.getString("recents","");if(!r.isEmpty())for(String s:r.split("\\|"))if(!s.isEmpty())recents.add(s);autoRetryEnabled=p.getBoolean("auto_retry",true);resizeMode=p.getInt("resize_mode",0);}
  void savePrefs(){getSharedPreferences("tvplus",MODE_PRIVATE).edit().putStringSet("favorites",new HashSet<>(favorites)).putString("recents",joinRecents()).putBoolean("auto_retry",autoRetryEnabled).putInt("resize_mode",resizeMode).apply();}
  String joinRecents(){StringBuilder b=new StringBuilder();for(String id:recents){if(b.length()>0)b.append("|");b.append(id);}return b.toString();}

  @Override public boolean dispatchKeyEvent(KeyEvent e){if(e.getAction()==KeyEvent.ACTION_DOWN){if(e.getKeyCode()==KeyEvent.KEYCODE_MEDIA_NEXT){step(1);return true;}if(e.getKeyCode()==KeyEvent.KEYCODE_MEDIA_PREVIOUS){step(-1);return true;}}return super.dispatchKeyEvent(e);}
  @Override public void onBackPressed(){confirmExit();}
  @Override protected void onDestroy(){super.onDestroy();if(player!=null)player.release();io.shutdownNow();}

  static final Pattern GROUP=Pattern.compile("group-title=\\\"([^\\\"]*)\\\""),ID=Pattern.compile("tvg-id=\\\"([^\\\"]*)\\\"");
  static List<Channel> parse(InputStream in)throws IOException{ArrayList<Channel> out=new ArrayList<>();if(in==null)return out;try(BufferedReader br=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){String line,meta=null;while((line=br.readLine())!=null){line=line.trim();if(line.startsWith("#EXTINF:"))meta=line;else if(!line.isEmpty()&&!line.startsWith("#")&&meta!=null){String name=meta.contains(",")?meta.substring(meta.lastIndexOf(',')+1).trim():"Canal";Matcher gm=GROUP.matcher(meta),im=ID.matcher(meta);String group=gm.find()?gm.group(1):"Otros";String id=im.find()&&im.group(1).trim().length()>0?im.group(1):sha1(name+"|"+line);out.add(new Channel(id,name,group,line));meta=null;}}}return out;}
  static String sha1(String s){try{byte[] b=MessageDigest.getInstance("SHA-1").digest(s.getBytes(StandardCharsets.UTF_8));StringBuilder x=new StringBuilder();for(byte v:b)x.append(String.format("%02x",v));return x.toString();}catch(Exception e){return Integer.toHexString(s.hashCode());}}
  static final class Channel{final String id,name,group,url,search;Channel(String i,String n,String g,String u){id=i;name=n;group=g;url=u;search=(n+" "+g).toLowerCase(Locale.ROOT);}}
}