import {
  AdditiveBlending,
  BufferAttribute,
  BufferGeometry,
  Color,
  PerspectiveCamera,
  Points,
  ShaderMaterial,
  Scene,
  WebGLRenderer,
} from 'three'

const VERTEX_SHADER =  `
uniform float uTime;
uniform float uDrift;
attribute float aSize;
attribute float aPhase;
varying float vPulse;

void main() {
  vec3 p = position;
  float t = uTime * 0.16 + aPhase;
  p.x += sin(t + position.y * 0.65) * uDrift;
  p.y += cos(t * 0.85 + position.x * 0.45) * uDrift * 0.72;
  vPulse = 0.55 + 0.45 * sin(t);
  vec4 mv = modelViewMatrix * vec4(p, 1.0);
  gl_PointSize = aSize * (280.0 / -mv.z);
  gl_Position = projectionMatrix * mv;
}
`

const FRAGMENT_SHADER =  `
uniform vec3 uColorA;
uniform vec3 uColorB;
varying float vPulse;

void main() {
  vec2 uv = gl_PointCoord - vec2(0.5);
  float d = length(uv);
  float alpha = smoothstep(0.5, 0.08, d) * (0.26 + vPulse * 0.48);
  vec3 color = mix(uColorA, uColorB, vPulse);
  gl_FragColor = vec4(color, alpha);
}
`

export interface FrontstageSceneHandles {
  dispose: () => void
  setPointer: (nx: number, ny: number) => void
  setPaused: (paused: boolean) => void
}

// 创建公开页面的轻量 WebGL 氛围层, 粒子数量和像素比都刻意收敛, 避免抢走内容阅读性能
// 初始化前台大背景的 WebGL 氛围粒子层渲染世界, 控制生命周期内的粒子更新与消隐
export function createFrontstageScene(container: HTMLElement): FrontstageSceneHandles {
  const renderer = new WebGLRenderer({ alpha: true, antialias: false, powerPreference: 'high-performance' })
  renderer.setClearColor(0x000000, 0)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 1.25))
  renderer.domElement.style.width = '100%'
  renderer.domElement.style.height = '100%'
  container.appendChild(renderer.domElement)

  const scene = new Scene()
  const camera = new PerspectiveCamera(42, 1, 0.1, 80)
  camera.position.set(0, 0, 18)

  const count = 360
  const positions = new Float32Array(count * 3)
  const sizes = new Float32Array(count)
  const phases = new Float32Array(count)

  for (let i = 0; i < count; i++) {
    const band = i % 3
    positions[i * 3] = (Math.random() - 0.5) * 24
    positions[i * 3 + 1] = (Math.random() - 0.5) * 18 + (band - 1) * 1.6
    positions[i * 3 + 2] = -Math.random() * 16
    sizes[i] = 0.045 + Math.random() * 0.075
    phases[i] = Math.random() * Math.PI * 2
  }

  const geometry = new BufferGeometry()
  geometry.setAttribute('position', new BufferAttribute(positions, 3))
  geometry.setAttribute('aSize', new BufferAttribute(sizes, 1))
  geometry.setAttribute('aPhase', new BufferAttribute(phases, 1))

  const material = new ShaderMaterial({
    vertexShader: VERTEX_SHADER,
    fragmentShader: FRAGMENT_SHADER,
    transparent: true,
    blending: AdditiveBlending,
    depthWrite: false,
    uniforms: {
      uTime: { value: 0 },
      uDrift: { value: 0.24 },
      uColorA: { value: new Color('#6ea8ff') },
      uColorB: { value: new Color('#ff9d6e') },
    },
  })

  const points = new Points(geometry, material)
  scene.add(points)

  const resize = () => {
    const width = container.clientWidth || 1
    const height = container.clientHeight || 1
    renderer.setSize(width, height, false)
    camera.aspect = width / height
    camera.updateProjectionMatrix()
  }

  const observer = new ResizeObserver(resize)
  observer.observe(container)
  resize()

  const pointer = { x: 0, y: 0 }
  const target = { x: 0, y: 0 }
  let lastTime = performance.now()
  let elapsed = 0
  let raf = 0
  let paused = false

  const render = () => {
    const now = performance.now()
    const dt = Math.min((now - lastTime) / 1000, 0.05)
    lastTime = now
    elapsed += dt
    material.uniforms.uTime!.value = elapsed
    pointer.x += (target.x - pointer.x) * 0.04
    pointer.y += (target.y - pointer.y) * 0.04
    points.rotation.y += dt * 0.018
    points.rotation.x = pointer.y * 0.035
    camera.position.x = pointer.x * 0.55
    camera.position.y = pointer.y * 0.38
    camera.lookAt(0, 0, 0)
    renderer.render(scene, camera)
    raf = requestAnimationFrame(render)
  }

  raf = requestAnimationFrame(render)

  const setPointer = (nx: number, ny: number) => {
    target.x = nx
    target.y = ny
  }

  const setPaused = (next: boolean) => {
    if (paused === next) {
      return
    }
    paused = next
    if (paused) {
      cancelAnimationFrame(raf)
      raf = 0
      return
    }
    lastTime = performance.now()
    raf = requestAnimationFrame(render)
  }

  const dispose = () => {
    if (raf) {
      cancelAnimationFrame(raf)
      raf = 0
    }
    observer.disconnect()
    geometry.dispose()
    material.dispose()
    renderer.dispose()
    if (renderer.domElement.parentNode === container) {
      container.removeChild(renderer.domElement)
    }
  }

  return { dispose, setPointer, setPaused }
}
