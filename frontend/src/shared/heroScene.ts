
import {
  AdditiveBlending,
  BufferAttribute,
  BufferGeometry,
  Color,
  IcosahedronGeometry,
  Mesh,
  PerspectiveCamera,
  Points,
  PointsMaterial,
  Scene,
  ShaderMaterial,
  WebGLRenderer,
} from 'three'
const GLSL_NOISE =  `
vec4 permute(vec4 x){return mod(((x*34.0)+1.0)*x,289.0);}
vec4 taylorInvSqrt(vec4 r){return 1.79284291400159-0.85373472095314*r;}
float snoise(vec3 v){
  const vec2 C=vec2(1.0/6.0,1.0/3.0);
  const vec4 D=vec4(0.0,0.5,1.0,2.0);
  vec3 i=floor(v+dot(v,C.yyy));
  vec3 x0=v-i+dot(i,C.xxx);
  vec3 g=step(x0.yzx,x0.xyz);
  vec3 l=1.0-g;
  vec3 i1=min(g.xyz,l.zxy);
  vec3 i2=max(g.xyz,l.zxy);
  vec3 x1=x0-i1+1.0*C.xxx;
  vec3 x2=x0-i2+2.0*C.xxx;
  vec3 x3=x0-1.0+3.0*C.xxx;
  i=mod(i,289.0);
  vec4 p=permute(permute(permute(
    i.z+vec4(0.0,i1.z,i2.z,1.0))
    +i.y+vec4(0.0,i1.y,i2.y,1.0))
    +i.x+vec4(0.0,i1.x,i2.x,1.0));
  float n_=1.0/7.0;
  vec3 ns=n_*D.wyz-D.xzx;
  vec4 j=p-49.0*floor(p*ns.z*ns.z);
  vec4 x_=floor(j*ns.z);
  vec4 y_=floor(j-7.0*x_);
  vec4 x=x_*ns.x+ns.yyyy;
  vec4 y=y_*ns.x+ns.yyyy;
  vec4 h=1.0-abs(x)-abs(y);
  vec4 b0=vec4(x.xy,y.xy);
  vec4 b1=vec4(x.zw,y.zw);
  vec4 s0=floor(b0)*2.0+1.0;
  vec4 s1=floor(b1)*2.0+1.0;
  vec4 sh=-step(h,vec4(0.0));
  vec4 a0=b0.xzyw+s0.xzyw*sh.xxyy;
  vec4 a1=b1.xzyw+s1.xzyw*sh.zzww;
  vec3 p0=vec3(a0.xy,h.x);
  vec3 p1=vec3(a0.zw,h.y);
  vec3 p2=vec3(a1.xy,h.z);
  vec3 p3=vec3(a1.zw,h.w);
  vec4 norm=taylorInvSqrt(vec4(dot(p0,p0),dot(p1,p1),dot(p2,p2),dot(p3,p3)));
  p0*=norm.x;p1*=norm.y;p2*=norm.z;p3*=norm.w;
  vec4 m=max(0.6-vec4(dot(x0,x0),dot(x1,x1),dot(x2,x2),dot(x3,x3)),0.0);
  m=m*m;
  return 42.0*dot(m*m,vec4(dot(p0,x0),dot(p1,x1),dot(p2,x2),dot(p3,x3)));
}
`

const VERTEX_SHADER =  `
uniform float uTime;
uniform float uAmp;
varying float vDisp;
varying vec3 vNormal;
varying vec3 vView;
${GLSL_NOISE}

void main(){
  float t = uTime * 0.18;
  float base = snoise(normal * 1.4 + t);
  float detail = snoise(normal * 3.1 - t * 1.4) * 0.4;
  float disp = (base + detail) * uAmp;
  vDisp = disp;
  vNormal = normalize(normalMatrix * normal);
  vec3 displaced = position + normal * disp;
  vec4 mv = modelViewMatrix * vec4(displaced, 1.0);
  vView = normalize(-mv.xyz);
  gl_Position = projectionMatrix * mv;
}
`

const FRAGMENT_SHADER =  `
uniform vec3 uColorA;
uniform vec3 uColorB;
uniform vec3 uColorC;
varying float vDisp;
varying vec3 vNormal;
varying vec3 vView;

void main(){
  float fres = pow(1.0 - max(dot(vNormal, vView), 0.0), 2.2);
  float m = smoothstep(-0.5, 0.6, vDisp);
  vec3 col = mix(uColorA, uColorB, m);
  col = mix(col, uColorC, fres * 0.85);
  col *= 0.3 + fres * 1.15;
  gl_FragColor = vec4(col, 1.0);
}
`

export interface HeroSceneHandles {
  dispose: () => void
  setPointer: (nx: number, ny: number) => void
  setPaused: (paused: boolean) => void
}

// 创建首页英雄区 WebGL 场景, 并返回外部可控的生命周期句柄
// 初始化首页引力 3D 网状模型交互场景并绑定窗口 resize 侦听
export function createHeroScene(container: HTMLElement): HeroSceneHandles {
  const renderer = new WebGLRenderer({ antialias: true, alpha: true, powerPreference: 'high-performance' })
  renderer.setClearColor(0x000000, 0)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 1.75))

  const scene = new Scene()
  const camera = new PerspectiveCamera(42, 1, 0.1, 100)
  camera.position.set(0, 0, 6.6)
  const coreGeo = new IcosahedronGeometry(1.7, 48)
  const coreMat = new ShaderMaterial({
    vertexShader: VERTEX_SHADER,
    fragmentShader: FRAGMENT_SHADER,
    uniforms: {
      uTime: { value: 0 },
      uAmp: { value: 0.36 },
      uColorA: { value: new Color('#0a1b3d') },
      uColorB: { value: new Color('#6e57ff') },
      uColorC: { value: new Color('#7fd4ff') },
    },
  })
  const core = new Mesh(coreGeo, coreMat)
  scene.add(core)
  const HALO_COUNT = 900
  const positions = new Float32Array(HALO_COUNT * 3)
  for (let i = 0; i < HALO_COUNT; i++) {
    const r = 2.6 + Math.random() * 2.4
    const theta = Math.random() * Math.PI * 2
    const phi = Math.acos(2 * Math.random() - 1)
    positions[i * 3] = r * Math.sin(phi) * Math.cos(theta)
    positions[i * 3 + 1] = r * Math.sin(phi) * Math.sin(theta)
    positions[i * 3 + 2] = r * Math.cos(phi)
  }
  const haloGeo = new BufferGeometry()
  haloGeo.setAttribute('position', new BufferAttribute(positions, 3))
  const haloMat = new PointsMaterial({
    size: 0.018,
    color: new Color('#9fc4ff'),
    transparent: true,
    opacity: 0.6,
    blending: AdditiveBlending,
    depthWrite: false,
  })
  const halo = new Points(haloGeo, haloMat)
  scene.add(halo)

  // 根据容器尺寸更新渲染器和相机比例
  const resize = () => {
    const w = container.clientWidth || 1
    const h = container.clientHeight || 1
    renderer.setSize(w, h, false)
    camera.aspect = w / h
    camera.updateProjectionMatrix()
  }
  renderer.domElement.style.width = '100%'
  renderer.domElement.style.height = '100%'
  container.appendChild(renderer.domElement)
  resize()

  const ro = new ResizeObserver(resize)
  ro.observe(container)
  const pointer = { x: 0, y: 0 }
  const target = { x: 0, y: 0 }

  // 保存最新指针位置, 渲染循环中会做缓动插值
  const setPointer = (nx: number, ny: number) => {
    target.x = nx
    target.y = ny
  }
  let lastTime = performance.now()
  let elapsed = 0
  let raf = 0
  let paused = false

  // 渲染一帧并安排下一帧动画
  const render = () => {
    const now = performance.now()
    const dt = Math.min((now - lastTime) / 1000, 0.05)
    lastTime = now
    elapsed += dt
    coreMat.uniforms.uTime!.value = elapsed
    core.rotation.y += dt * 0.12
    core.rotation.z += dt * 0.03
    halo.rotation.y -= dt * 0.04
    pointer.x += (target.x - pointer.x) * 0.05
    pointer.y += (target.y - pointer.y) * 0.05
    camera.position.x = pointer.x * 0.9
    camera.position.y = pointer.y * 0.6
    camera.lookAt(0, 0, 0)

    renderer.render(scene, camera)
    raf = requestAnimationFrame(render)
  }
  raf = requestAnimationFrame(render)

  // 根据可见性暂停或恢复动画, 减少不可见区域的渲染开销
  const setPaused = (next: boolean) => {
    if (next === paused) {
      return
    }
    paused = next
    if (paused) {
      cancelAnimationFrame(raf)
      raf = 0
    } else {
      // 丢弃暂停期间的长时间差, 避免恢复时动画突然跳变
      lastTime = performance.now()
      raf = requestAnimationFrame(render)
    }
  }

  // 释放 Three.js 资源和挂载到容器内的 canvas
  const dispose = () => {
    if (raf) {
      cancelAnimationFrame(raf)
      raf = 0
    }
    ro.disconnect()
    coreGeo.dispose()
    coreMat.dispose()
    haloGeo.dispose()
    haloMat.dispose()
    renderer.dispose()
    if (renderer.domElement.parentNode === container) {
      container.removeChild(renderer.domElement)
    }
  }

  return { dispose, setPointer, setPaused }
}
