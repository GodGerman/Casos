import React, { useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import AlertMessage from '../components/AlertMessage.jsx';
import Loading from '../components/Loading.jsx';
import Palette from '../components/Palette.jsx';
import Canvas from '../components/Canvas.jsx';
import {
  actualizarConexion,
  actualizarDiagrama,
  actualizarElemento,
  crearConexion,
  crearElemento,
  eliminarConexion,
  eliminarElemento,
  listarConexiones,
  listarElementos,
  obtenerDiagrama
} from '../services/diagramas.js';
import ElementIcon from '../components/ElementIcon.jsx';
import { validarConexion, validarDiagrama, validarElemento } from '../utils/validators.js';

const ESTADOS = [
  { value: 'ACTIVO', label: 'Activo' },
  { value: 'BORRADOR', label: 'Borrador' },
  { value: 'ARCHIVADO', label: 'Archivado' }
];

const CONEXIONES = [
  { value: 'ASOCIACION', label: 'Asociación' },
  { value: 'INCLUSION', label: 'Inclusión' },
  { value: 'EXTENSION', label: 'Extensión' },
  { value: 'GENERALIZACION', label: 'Generalización' },
  { value: 'DEPENDENCIA', label: 'Dependencia' },
  { value: 'ENLACE_NOTA', label: 'Enlace Nota' }
];

const TIPOS_ELEMENTO = [
  { value: 'ACTOR', label: 'Actor' },
  { value: 'CASO_DE_USO', label: 'Caso de Uso' },
  { value: 'LIMITE_SISTEMA', label: 'Límite Sistema' },
  { value: 'PAQUETE', label: 'Paquete' },
  { value: 'NOTA', label: 'Nota' },
  { value: 'TEXTO', label: 'Texto' },
  { value: 'IMAGEN', label: 'Imagen' }
];

export default function DiagramEditorPage() {
  const { id: id_diagrama } = useParams();
  const canvasRef = useRef(null);
  const elementsRef = useRef([]);

  const [diagrama, setDiagrama] = useState(null);
  const [elementos, setElementos] = useState([]);
  const [conexiones, setConexiones] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [dragging, setDragging] = useState(null);
  const [selectedElementId, setSelectedElementId] = useState(null);
  const [elementForm, setElementForm] = useState(null);
  const [selectedConnectionId, setSelectedConnectionId] = useState(null);
  const [connectionForm, setConnectionForm] = useState(null);
  const [tipo_conexion, set_tipo_conexion] = useState('ASOCIACION');
  const [selectedForConnection, setSelectedForConnection] = useState([]);
  const [newConnectionLabel, setNewConnectionLabel] = useState('');
  const [showAdvancedDiagram, setShowAdvancedDiagram] = useState(false);
  const [showAdvancedElement, setShowAdvancedElement] = useState(false);
  const [showAdvancedConnection, setShowAdvancedConnection] = useState(false);

  const [showMobileTools, setShowMobileTools] = useState(false);
  const [showMobileProps, setShowMobileProps] = useState(false);

  // Sync ref
  useEffect(() => {
    elementsRef.current = elementos;
  }, [elementos]);

  // Load selected element into form
  useEffect(() => {
    if (!selectedElementId) {
      setElementForm(null);
      return;
    }
    const selected = elementos.find((el) => el.id_elemento === selectedElementId);
    if (selected) {
      setElementForm({ ...selected });
      // Deselect connection if element is selected
      setSelectedConnectionId(null);
      // Open properties drawer on mobile when element selected
      if (window.innerWidth < 992) setShowMobileProps(true);
    }
  }, [selectedElementId, elementos]);

  // Load selected connection into form
  useEffect(() => {
    if (!selectedConnectionId) {
      setConnectionForm(null);
      return;
    }
    const selected = conexiones.find((conn) => conn.id_conexion === selectedConnectionId);
    if (selected) {
      setConnectionForm({ ...selected });
      // Deselect element if connection is selected
      setSelectedElementId(null);
      // Open properties drawer on mobile when connection selected
      if (window.innerWidth < 992) setShowMobileProps(true);
    } else {
      setSelectedConnectionId(null);
      setConnectionForm(null);
    }
  }, [selectedConnectionId, conexiones]);

  // Filter connection selection candidates
  useEffect(() => {
    setSelectedForConnection((prev) => prev.filter((idItem) => elementos.some((el) => el.id_elemento === idItem)));
  }, [elementos]);

  const cargar = async () => {
    setLoading(true);
    setError('');
    try {
      const dataDiagrama = await obtenerDiagrama(id_diagrama);
      setDiagrama(dataDiagrama.diagrama);
      const dataElementos = await listarElementos(id_diagrama);
      setElementos(dataElementos.elementos || []);
      const dataConexiones = await listarConexiones(id_diagrama);
      setConexiones(dataConexiones.conexiones || []);
    } catch (err) {
      setError(err?.data?.mensaje || 'No se pudo cargar el diagrama.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    cargar();
  }, [id_diagrama]);

  const handleDiagramChange = (event) => {
    const { name, value } = event.target;
    setDiagrama((prev) => ({ ...prev, [name]: value }));
  };

  const handleGuardarDiagrama = async () => {
    if (!diagrama) return;
    setError('');
    setSuccess('');
    const errores = validarDiagrama(diagrama);
    if (errores.length > 0) {
      setError(errores.join(' '));
      return;
    }
    try {
      await actualizarDiagrama({
        id_diagrama: diagrama.id_diagrama,
        nombre: diagrama.nombre,
        descripcion: diagrama.descripcion,
        estado: diagrama.estado,
        ancho_lienzo: Number(diagrama.ancho_lienzo),
        alto_lienzo: Number(diagrama.alto_lienzo),
        configuracion_json: diagrama.configuracion_json || null
      });
      setSuccess('Diagrama actualizado.');
    } catch (err) {
      setError(err?.data?.mensaje || 'No se pudo actualizar el diagrama.');
    }
  };

  const handleDrop = async (event) => {
    event.preventDefault();
    const tipo_elemento = event.dataTransfer.getData('tipo_elemento');
    if (!tipo_elemento || tipo_elemento === 'RELACION') {
      return;
    }
    if (!diagrama || !canvasRef.current) return;

    // Adjust coordinates based on scroll/offset
    const rect = canvasRef.current.getBoundingClientRect();

    // Simple calculation: mouse pos relative to canvas container + scroll
    const pos_x = Math.max(0, event.clientX - rect.left);
    const pos_y = Math.max(0, event.clientY - rect.top);

    // Default dimensions by type
    const getDefaultDimensions = (type) => {
      switch (type) {
        case 'ACTOR': return { w: 60, h: 100 };
        case 'CASO_DE_USO': return { w: 140, h: 70 };
        case 'PAQUETE': return { w: 200, h: 120 };
        case 'LIMITE_SISTEMA': return { w: 300, h: 400 };
        case 'NOTA': return { w: 160, h: 100 };
        case 'TEXTO': return { w: 120, h: 40 };
        case 'IMAGEN': return { w: 200, h: 150 };
        default: return { w: 140, h: 70 };
      }
    };

    const dims = getDefaultDimensions(tipo_elemento);

    try {
      const result = await crearElemento({
        id_diagrama: diagrama.id_diagrama,
        tipo_elemento: tipo_elemento,
        etiqueta: '',
        pos_x: Math.round(pos_x),
        pos_y: Math.round(pos_y),
        ancho: dims.w,
        alto: dims.h,
        rotacion_grados: 0,
        orden_z: 0,
        estilo_json: null,
        metadatos_json: null
      });
      if (result?.id_elemento) {
        await cargar();
        if (window.innerWidth < 992) {
          setShowMobileTools(false); // Close tools after drop
          setShowMobileProps(true); // Open properties to edit new element
        }
      }
    } catch (err) {
      console.error(err);
      setError('No se pudo crear el elemento.');
    }
  };

  const handleDragOver = (event) => {
    event.preventDefault();
  };

  const handleElementMouseDown = (event, elemento) => {
    event.preventDefault();
    event.stopPropagation();
    const rect = canvasRef.current.getBoundingClientRect();
    setDragging({
      id_elemento: elemento.id_elemento,
      offsetX: event.clientX - rect.left - elemento.pos_x,
      offsetY: event.clientY - rect.top - elemento.pos_y
    });
    // Select on mouse down
    setSelectedElementId(elemento.id_elemento);
  };

  const handleElementClick = (event, elemento) => {
    event.stopPropagation();
    setSelectedElementId(elemento.id_elemento);
  };

  useEffect(() => {
    if (!dragging) return undefined;

    const onMove = (event) => {
      if (!canvasRef.current) return;
      const rect = canvasRef.current.getBoundingClientRect();
      // Handle touch or mouse
      const clientX = event.touches ? event.touches[0].clientX : event.clientX;
      const clientY = event.touches ? event.touches[0].clientY : event.clientY;

      const nextX = Math.max(0, clientX - rect.left - dragging.offsetX);
      const nextY = Math.max(0, clientY - rect.top - dragging.offsetY);

      setElementos((prev) => prev.map((el) => (
        el.id_elemento === dragging.id_elemento
          ? { ...el, pos_x: Math.round(nextX), pos_y: Math.round(nextY) }
          : el
      )));
    };

    const onUp = async () => {
      const elemento = elementsRef.current.find((el) => el.id_elemento === dragging.id_elemento);
      setDragging(null);
      if (!elemento) return;

      try {
        await actualizarElemento({ ...elemento });
      } catch (err) {
        setError('No se pudo guardar la posición.');
      }
    };

    window.addEventListener('mousemove', onMove);
    window.addEventListener('mouseup', onUp);
    // Touch events for mobile dragging
    window.addEventListener('touchmove', onMove, { passive: false });
    window.addEventListener('touchend', onUp);

    return () => {
      window.removeEventListener('mousemove', onMove);
      window.removeEventListener('mouseup', onUp);
      window.removeEventListener('touchmove', onMove);
      window.removeEventListener('touchend', onUp);
    };
  }, [dragging]);

  const handleElementFormChange = (event) => {
    const { name, value } = event.target;
    setElementForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleGuardarElemento = async () => {
    if (!elementForm) return;
    const errores = validarElemento(elementForm);
    if (errores.length > 0) {
      setError(errores.join(' '));
      return;
    }
    try {
      await actualizarElemento({
        ...elementForm,
        id_elemento_padre: elementForm.id_elemento_padre ? Number(elementForm.id_elemento_padre) : null,
        pos_x: Number(elementForm.pos_x),
        pos_y: Number(elementForm.pos_y),
        ancho: Number(elementForm.ancho),
        alto: Number(elementForm.alto),
        rotacion_grados: Number(elementForm.rotacion_grados || 0),
        orden_z: Number(elementForm.orden_z || 0)
      });
      setSuccess('Elemento actualizado.');
      await cargar();
    } catch (err) {
      setError(err?.data?.mensaje || 'No se pudo actualizar el elemento.');
    }
  };

  const handleEliminarElemento = async () => {
    if (!selectedElementId) return;
    if (!window.confirm('¿Deseas eliminar el elemento seleccionado?')) return;
    try {
      await eliminarElemento(selectedElementId);
      setSelectedElementId(null);
      setElementForm(null);
      await cargar();
    } catch (err) {
      setError(err?.data?.mensaje || 'No se pudo eliminar el elemento.');
    }
  };

  const toggleConnectionSelection = (id_elemento) => {
    setSelectedForConnection((prev) => {
      if (prev.includes(id_elemento)) return prev.filter((id) => id !== id_elemento);
      if (prev.length >= 2) return prev;
      return [...prev, id_elemento];
    });
  };

  const handleCrearConexion = async () => {
    if (selectedForConnection.length !== 2) {
      setError('Selecciona dos elementos para conectar.');
      return;
    }
    try {
      await crearConexion({
        id_diagrama: diagrama.id_diagrama,
        id_elemento_origen: selectedForConnection[0],
        id_elemento_destino: selectedForConnection[1],
        tipo_conexion: tipo_conexion,
        etiqueta: newConnectionLabel.trim() || null,
        puntos_json: null,
        estilo_json: null
      });
      setNewConnectionLabel('');
      setSelectedForConnection([]);
      await cargar();
      setSuccess('Conexión creada');
    } catch (err) {
      setError(err?.data?.mensaje || 'No se pudo crear la conexión.');
    }
  };

  const handleConnectionSelect = (conexion) => {
    setSelectedConnectionId(conexion.id_conexion);
  };

  const handleConnectionFormChange = (event) => {
    const { name, value } = event.target;
    setConnectionForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleGuardarConexion = async () => {
    if (!connectionForm || !diagrama) return;
    const errores = validarConexion(connectionForm);
    if (errores.length > 0) {
      setError(errores.join(' '));
      return;
    }
    try {
      await actualizarConexion({
        id_conexion: connectionForm.id_conexion,
        id_diagrama: diagrama.id_diagrama,
        id_elemento_origen: Number(connectionForm.id_elemento_origen),
        id_elemento_destino: Number(connectionForm.id_elemento_destino),
        tipo_conexion: connectionForm.tipo_conexion,
        etiqueta: connectionForm.etiqueta ? connectionForm.etiqueta.trim() : null,
        puntos_json: connectionForm.puntos_json ? connectionForm.puntos_json.trim() : null,
        estilo_json: connectionForm.estilo_json ? connectionForm.estilo_json.trim() : null
      });
      setSuccess('Conexión actualizada.');
      await cargar();
    } catch (err) {
      setError(err?.data?.mensaje || 'No se pudo actualizar la conexión.');
    }
  };

  const handleEliminarConexion = async (id_conexion) => {
    if (!window.confirm('¿Eliminar esta conexión?')) return;
    try {
      await eliminarConexion(id_conexion);
      if (selectedConnectionId === id_conexion) {
        setSelectedConnectionId(null);
        setConnectionForm(null);
      }
      await cargar();
    } catch (err) {
      setError(err?.data?.mensaje || 'No se pudo eliminar la conexión.');
    }
  };

  // Left Sidebar Content (reusable)
  const LeftSidebarContent = () => (
    <>
      <div className="p-3 border-bottom border-dark-700">
        <Link to="/diagramas" className="d-flex align-items-center gap-2 text-decoration-none text-secondary hover-text-primary mb-3 text-sm">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="19" y1="12" x2="5" y2="12"></line><polyline points="12 19 5 12 12 5"></polyline></svg>
          Volver
        </Link>
        <h6 className="mb-1 text-white text-truncate" title={diagrama.nombre}>{diagrama.nombre}</h6>
        <span className="badge bg-primary-900 text-primary-300 border border-primary-700">{diagrama.estado}</span>
      </div>

      <div className="flex-grow-1 overflow-y-auto p-3">
        <Palette />

        {/* Connection Tool */}
        <div className="card shadow-sm border-0 bg-dark-800 mt-3">
          <div className="card-header bg-transparent border-dark-700 py-2">
            <span className="text-xs text-uppercase text-secondary fw-bold">Crear Relación</span>
          </div>
          <div className="card-body p-2">
            <div className="mb-2">
              <select className="form-select form-select-sm" value={tipo_conexion} onChange={(e) => set_tipo_conexion(e.target.value)}>
                {CONEXIONES.map(c => <option key={c.value} value={c.value}>{c.label}</option>)}
              </select>
            </div>
            <div className="mb-2">
              <input className="form-control form-control-sm" placeholder="Etiqueta (opcional)" value={newConnectionLabel} onChange={(e) => setNewConnectionLabel(e.target.value)} />
            </div>
            <button className="btn btn-primary btn-sm w-100" onClick={handleCrearConexion} disabled={selectedForConnection.length !== 2}>
              {selectedForConnection.length === 2 ? 'Conectar' : 'Selecciona 2 elementos'}
            </button>
            <div className="mt-2 text-xs text-secondary text-center">
              {selectedForConnection.length === 0 && "Click en 2 elementos"}
              {selectedForConnection.length === 1 && "Selecciona 1 más..."}
              {selectedForConnection.length === 2 && "¡Listo para conectar!"}
            </div>
          </div>
        </div>
      </div>
    </>
  );

  // Right Sidebar Content (reusable)
  const RightSidebarContent = () => (
    <>
      {/* Dynamic Property Inspector */}
      <div className="flex-grow-1 overflow-y-auto p-3">
        {!selectedElementId && !selectedConnectionId && (
          <div className="text-center text-muted py-5 text-sm">
            <svg className="mb-3 opacity-25" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1"><rect x="3" y="3" width="18" height="18" rx="2"></rect><circle cx="12" cy="12" r="3"></circle></svg>
            <p>Selecciona un elemento o conexión para editar sus propiedades.</p>
            <button className="btn btn-outline-secondary btn-sm mt-2" onClick={() => setShowAdvancedDiagram(!showAdvancedDiagram)}>
              Configurar Diagrama
            </button>

            {showAdvancedDiagram && (
              <div className="mt-3 text-start animate-fade-in">
                <div className="mb-2">
                  <label className="form-label text-xs">Ancho x Alto</label>
                  <div className="d-flex gap-1">
                    <input name="ancho_lienzo" type="number" className="form-control form-control-sm" value={diagrama.ancho_lienzo} onChange={handleDiagramChange} />
                    <input name="alto_lienzo" type="number" className="form-control form-control-sm" value={diagrama.alto_lienzo} onChange={handleDiagramChange} />
                  </div>
                </div>
                <button className="btn btn-primary btn-sm w-100" onClick={handleGuardarDiagrama}>Guardar Cambios</button>
              </div>
            )}
          </div>
        )}

        {/* ELEMENT EDITOR */}
        {elementForm && (
          <div className="card shadow-sm border-0 bg-dark-800">
            <div className="card-header bg-transparent border-dark-700 py-2">
              <span className="text-xs text-uppercase text-primary-400 fw-bold">Propiedades Elemento</span>
            </div>
            <div className="card-body p-2 gap-2 d-flex flex-column">
              <div className="d-flex gap-2">
                <div className="flex-grow-1">
                  <label className="form-label text-xs text-secondary mb-1">Tipo</label>
                  <select className="form-select form-select-sm" name="tipo_elemento" value={elementForm.tipo_elemento} onChange={handleElementFormChange}>
                    {TIPOS_ELEMENTO.map(t => <option key={t.value} value={t.value}>{t.label}</option>)}
                  </select>
                </div>
              </div>
              <div>
                <label className="form-label text-xs text-secondary mb-1">Nombre / Etiqueta</label>
                <input className="form-control form-control-sm" name="etiqueta" value={elementForm.etiqueta || ''} onChange={handleElementFormChange} />
              </div>

              <div className="row g-1">
                <div className="col-6">
                  <label className="form-label text-xs text-secondary mb-1">X</label>
                  <input type="number" className="form-control form-control-sm" name="pos_x" value={elementForm.pos_x} onChange={handleElementFormChange} />
                </div>
                <div className="col-6">
                  <label className="form-label text-xs text-secondary mb-1">Y</label>
                  <input type="number" className="form-control form-control-sm" name="pos_y" value={elementForm.pos_y} onChange={handleElementFormChange} />
                </div>
                <div className="col-6">
                  <label className="form-label text-xs text-secondary mb-1">W</label>
                  <input type="number" className="form-control form-control-sm" name="ancho" value={elementForm.ancho} onChange={handleElementFormChange} />
                </div>
                <div className="col-6">
                  <label className="form-label text-xs text-secondary mb-1">H</label>
                  <input type="number" className="form-control form-control-sm" name="alto" value={elementForm.alto} onChange={handleElementFormChange} />
                </div>
              </div>

              <div className="d-flex gap-2 mt-3">
                <button className="btn btn-primary btn-sm flex-grow-1" onClick={handleGuardarElemento}>Guardar</button>
                <button className="btn btn-outline-danger btn-sm" onClick={handleEliminarElemento} title="Eliminar">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>
                </button>
              </div>
            </div>
          </div>
        )}

        {/* CONNECTION EDITOR */}
        {connectionForm && (
          <div className="card shadow-sm border-0 bg-dark-800">
            <div className="card-header bg-transparent border-dark-700 py-2">
              <span className="text-xs text-uppercase text-primary-400 fw-bold">Propiedades Conexión</span>
            </div>
            <div className="card-body p-2 gap-2 d-flex flex-column">
              <div>
                <label className="form-label text-xs text-secondary mb-1">Tipo</label>
                <select className="form-select form-select-sm" name="tipo_conexion" value={connectionForm.tipo_conexion} onChange={handleConnectionFormChange}>
                  {CONEXIONES.map(t => <option key={t.value} value={t.value}>{t.label}</option>)}
                </select>
              </div>
              <div>
                <label className="form-label text-xs text-secondary mb-1">Etiqueta</label>
                <input className="form-control form-control-sm" name="etiqueta" value={connectionForm.etiqueta || ''} onChange={handleConnectionFormChange} />
              </div>
              <div className="d-flex gap-2 mt-3">
                <button className="btn btn-primary btn-sm flex-grow-1" onClick={handleGuardarConexion}>Guardar</button>
                <button className="btn btn-outline-danger btn-sm" onClick={() => handleEliminarConexion(connectionForm.id_conexion)}>Eliminar</button>
              </div>
            </div>
          </div>
        )}

      </div>

      {/* ELEMENT LIST / LAYERS PANEL */}
      <div className="border-top border-dark-700 flex-shrink-0" style={{ height: '35%' }}>
        <div className="px-3 py-2 bg-dark-800 border-bottom border-dark-700 d-flex justify-content-between align-items-center">
          <span className="text-xs fw-bold text-secondary text-uppercase">Capas</span>
          <span className="text-xs text-muted">{elementos.length} els</span>
        </div>
        <div className="overflow-y-auto h-100 list-group list-group-flush">
          {elementos.map(el => (
            <div
              key={el.id_elemento}
              className={`list-group-item bg-transparent border-0 py-1 px-3 d-flex align-items-center gap-2 cursor-pointer hover-bg-dark-800 ${selectedElementId === el.id_elemento ? 'bg-dark-800 border-start border-3 border-primary-500' : ''}`}
              onClick={(e) => handleElementClick(e, el)}
            >
              <input
                type="checkbox"
                className="form-check-input mt-0"
                checked={selectedForConnection.includes(el.id_elemento)}
                onChange={() => toggleConnectionSelection(el.id_elemento)}
                onClick={(e) => e.stopPropagation()}
                title="Seleccionar para conectar"
              />
              <span className="text-muted"><ElementIcon type={el.tipo_elemento} size={14} /></span>
              <span className="text-xs text-secondary text-truncate flex-grow-1">{el.etiqueta || el.tipo_elemento}</span>
            </div>
          ))}
        </div>
      </div>
    </>
  );

  // Main UI Render
  return (
    <div className="d-flex flex-column h-100 position-relative" style={{ minHeight: 'calc(100vh - 80px)' }}>

      {/* Messages Toast Area (Simplified) */}
      {(error || success) && (
        <div className="position-fixed top-0 start-50 translate-middle-x mt-3 z-30" style={{ minWidth: '300px' }}>
          <AlertMessage type="danger" message={error} />
          <AlertMessage type="success" message={success} />
        </div>
      )}

      {loading && <div className="text-center py-5"><Loading text="Cargando editor..." /></div>}

      {!loading && diagrama && (
        <div className="row g-0 flex-grow-1 h-100 position-relative">

          {/* LEFT SIDEBAR: Tools & Diagram Info (Desktop: Col / Mobile: Drawer) */}
          <div className={`col-lg-2 d-none d-lg-flex flex-column bg-dark-900 border-end border-dark-700 h-100 overflow-hidden`}>
            <LeftSidebarContent />
          </div>

          {/* Mobile Left Drawer */}
          <div className={`mobile-drawer mobile-drawer-left d-lg-none ${showMobileTools ? 'show' : ''}`}>
            <div className="d-flex flex-column h-100">
              <div className="p-2 border-bottom border-dark-700 d-flex justify-content-between align-items-center">
                <span className="fw-bold text-sm px-2">Herramientas</span>
                <button className="btn btn-sm text-secondary" onClick={() => setShowMobileTools(false)}>✕</button>
              </div>
              <LeftSidebarContent />
            </div>
          </div>

          {/* CENTER: Canvas Area */}
          <div className="col-12 col-lg-7 col-xl-8 bg-dark-900 position-relative overflow-auto h-100 p-0" style={{ display: 'grid', gridTemplateColumns: 'min-content min-content', placeContent: 'center' }}>
            <div className="p-4 d-inline-block">
              <Canvas
                canvasRef={canvasRef}
                width={Number(diagrama.ancho_lienzo)}
                height={Number(diagrama.alto_lienzo)}
                elements={elementos}
                connections={conexiones}
                selectedElementId={selectedElementId}
                onDrop={handleDrop}
                onDragOver={handleDragOver}
                onElementMouseDown={handleElementMouseDown}
                onElementClick={handleElementClick}
              />
            </div>
          </div>

          {/* RIGHT SIDEBAR: Properties & Layers (Desktop: Col / Mobile: Drawer) */}
          <div className={`col-lg-3 col-xl-2 d-none d-lg-flex flex-column bg-dark-900 border-start border-dark-700 h-100 overflow-hidden`}>
            <RightSidebarContent />
          </div>

          {/* Mobile Right Drawer */}
          <div className={`mobile-drawer mobile-drawer-right d-lg-none ${showMobileProps ? 'show' : ''}`}>
            <div className="d-flex flex-column h-100">
              <div className="p-2 border-bottom border-dark-700 d-flex justify-content-between align-items-center">
                <span className="fw-bold text-sm px-2">Propiedades</span>
                <button className="btn btn-sm text-secondary" onClick={() => setShowMobileProps(false)}>✕</button>
              </div>
              <RightSidebarContent />
            </div>
          </div>

          {/* Mobile Backdrop */}
          <div className={`mobile-backdrop d-lg-none ${showMobileTools || showMobileProps ? 'show' : ''}`} onClick={() => { setShowMobileTools(false); setShowMobileProps(false); }}></div>

          {/* Mobile Bottom Toolbar */}
          <div className="mobile-toolbar d-lg-none">
            <button className={`mobile-toolbar-btn ${showMobileTools ? 'active' : ''}`} onClick={() => { setShowMobileTools(!showMobileTools); setShowMobileProps(false); }}>
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect><rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect></svg>
              Herramientas
            </button>
            <button className="mobile-toolbar-btn" onClick={() => window.history.back()}>
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="19" y1="12" x2="5" y2="12"></line><polyline points="12 19 5 12 12 5"></polyline></svg>
              Volver
            </button>
            <button className={`mobile-toolbar-btn ${showMobileProps ? 'active' : ''}`} onClick={() => { setShowMobileProps(!showMobileProps); setShowMobileTools(false); }}>
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="4" y1="21" x2="4" y2="14"></line><line x1="4" y1="10" x2="4" y2="3"></line><line x1="12" y1="21" x2="12" y2="12"></line><line x1="12" y1="8" x2="12" y2="3"></line><line x1="20" y1="21" x2="20" y2="16"></line><line x1="20" y1="12" x2="20" y2="3"></line><line x1="1" y1="14" x2="7" y2="14"></line><line x1="9" y1="8" x2="15" y2="8"></line><line x1="17" y1="16" x2="23" y2="16"></line></svg>
              Propiedades
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
