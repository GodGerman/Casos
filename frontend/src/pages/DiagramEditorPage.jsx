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

// Catalogo de estados para el formulario de diagrama.
const ESTADOS = [
  { value: 'ACTIVO', label: 'Activo' },
  { value: 'BORRADOR', label: 'Borrador' },
  { value: 'ARCHIVADO', label: 'Archivado' }
];

// Tipos de conexion soportados por backend y renderer.
const CONEXIONES = [
  { value: 'ASOCIACION', label: 'Asociación' },
  { value: 'INCLUSION', label: 'Inclusión' },
  { value: 'EXTENSION', label: 'Extensión' },
  { value: 'GENERALIZACION', label: 'Generalización' },
  { value: 'DEPENDENCIA', label: 'Dependencia' },
  { value: 'ENLACE_NOTA', label: 'Enlace Nota' }
];

// Tipos de elemento disponibles en el editor.
const TIPOS_ELEMENTO = [
  { value: 'ACTOR', label: 'Actor' },
  { value: 'CASO_DE_USO', label: 'Caso de Uso' },
  { value: 'LIMITE_SISTEMA', label: 'Límite Sistema' },
  { value: 'PAQUETE', label: 'Paquete' },
  { value: 'NOTA', label: 'Nota' },
  { value: 'TEXTO', label: 'Texto' },
  { value: 'IMAGEN', label: 'Imagen' }
];

/**
 * Editor de diagramas UML: canvas, herramientas y propiedades.
 *
 * Se carga diagrama/elementos/conexiones desde el backend,
 * permite crear elementos via drag & drop, moverlos con drag, y editar
 * propiedades en paneles laterales (desktop) o drawers (mobile).
 *
 *
 * @returns {JSX.Element} pagina completa del editor.
 */
export default function DiagramEditorPage() {
  const { id: id_diagrama } = useParams();
  // Referencia al canvas para calcular offsets y posiciones.
  const canvasRef = useRef(null);
  // Ref para acceder a elementos durante drag sin re-render sincronico.
  const elementsRef = useRef([]);

  const [diagrama, setDiagrama] = useState(null);
  const [elementos, setElementos] = useState([]);
  const [conexiones, setConexiones] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  // dragging guarda el id del elemento y offset del puntero durante el drag.
  const [dragging, setDragging] = useState(null);
  const [selectedElementId, setSelectedElementId] = useState(null);
  const [elementForm, setElementForm] = useState(null);
  const [selectedConnectionId, setSelectedConnectionId] = useState(null);
  const [connectionForm, setConnectionForm] = useState(null);
  const [tipo_conexion, set_tipo_conexion] = useState('ASOCIACION');
  // Lista temporal de elementos seleccionados para crear una conexion (max 2).
  const [selectedForConnection, setSelectedForConnection] = useState([]);
  const [newConnectionLabel, setNewConnectionLabel] = useState('');
  const [showAdvancedDiagram, setShowAdvancedDiagram] = useState(false);
  const [showAdvancedElement, setShowAdvancedElement] = useState(false);
  const [showAdvancedConnection, setShowAdvancedConnection] = useState(false);

  const [showMobileTools, setShowMobileTools] = useState(false);
  const [showMobileProps, setShowMobileProps] = useState(false);

  // Sincroniza la referencia mutable con el ultimo estado para usarla en onUp.
  useEffect(() => {
    elementsRef.current = elementos;
  }, [elementos]);

  // Carga el elemento seleccionado en el formulario de propiedades.
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

  // Carga la conexion seleccionada en el formulario de propiedades.
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

  // Mantiene seleccion para conexiones solo con elementos existentes.
  useEffect(() => {
    setSelectedForConnection((prev) => prev.filter((idItem) => elementos.some((el) => el.id_elemento === idItem)));
  }, [elementos]);

  /**
   * Carga diagrama, elementos y conexiones desde el backend.
   *
   * @returns {Promise<void>} no retorna valor; actualiza estado local.
   * Si falla la red o el backend, actualiza el mensaje de error.
   *
   * Se llama a tres endpoints (diagrama, elementos, conexiones)
   * y actualiza el estado en serie para que el canvas tenga datos coherentes.
   *
   */
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

  /**
   * Actualiza estado local del diagrama a partir de inputs del formulario.
   *
   * @param {Event} event evento de cambio en inputs.
   * @returns {void} no retorna valor; solo actualiza estado.
   *
   * Se usa name/value del input para sobrescribir el campo
   * correspondiente en el objeto diagrama.
   *
   */
  const handleDiagramChange = (event) => {
    const { name, value } = event.target;
    setDiagrama((prev) => ({ ...prev, [name]: value }));
  };

  /**
   * Guarda cambios de configuracion del diagrama.
   *
   * @returns {Promise<void>} no retorna valor; muestra mensaje de exito/error.
   * Si falla la validacion o el backend rechaza, muestra el error.
   *
   * Se validan campos locales, se normalizan tipos numericos y
   * se llama al endpoint de actualizacion.
   *
   */
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

  /**
   * Crea un elemento al soltar desde la paleta sobre el canvas.
   *
   * @param {DragEvent} event evento de drop.
   * @returns {Promise<void>} no retorna valor; actualiza elementos.
   * Si el backend falla, actualiza el mensaje de error.
   *
   * Se lee el tipo desde dataTransfer, calcula posicion relativa
   * al canvas con getBoundingClientRect y envía el payload al backend.
   *
   */
  const handleDrop = async (event) => {
    event.preventDefault();
    const tipo_elemento = event.dataTransfer.getData('tipo_elemento');
    if (!tipo_elemento || tipo_elemento === 'RELACION') {
      return;
    }
    if (!diagrama || !canvasRef.current) return;

    // Ajusta coordenadas segun posicion del canvas en pantalla.
    const rect = canvasRef.current.getBoundingClientRect();

    // Posicion relativa dentro del canvas.
    const pos_x = Math.max(0, event.clientX - rect.left);
    const pos_y = Math.max(0, event.clientY - rect.top);

    // Dimensiones por defecto segun tipo de elemento.
    /**
     * Calcula dimensiones base para el tipo de elemento.
     *
     * @param {string} type tipo de elemento UML.
     * @returns {{w: number, h: number}} dimensiones recomendadas.
     */
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

  /**
   * Permite que el canvas acepte elementos drag & drop.
   *
   * @param {DragEvent} event evento de dragover.
   * @returns {void} no retorna valor; solo evita el comportamiento default.
   *
   * Se llama a preventDefault para habilitar el drop en el canvas.
   *
   */
  const handleDragOver = (event) => {
    event.preventDefault();
  };

  /**
   * Inicia el drag de un elemento existente en el canvas.
   *
   * @param {MouseEvent} event evento de mouse down.
   * @param {object} elemento elemento seleccionado.
   * @returns {void} no retorna valor; prepara estado de drag.
   *
   * Se calcula el offset del puntero respecto al elemento
   * para mantener la misma distancia durante el movimiento.
   *
   */
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

  /**
   * Selecciona un elemento para edicion de propiedades.
   *
   * @param {MouseEvent} event evento de click.
   * @param {object} elemento elemento seleccionado.
   * @returns {void} no retorna valor; actualiza estado de seleccion.
   *
   * Se evita propagacion para no perder seleccion
   * y guarda el id del elemento en el estado.
   *
   */
  const handleElementClick = (event, elemento) => {
    event.stopPropagation();
    setSelectedElementId(elemento.id_elemento);
  };

  useEffect(() => {
    if (!dragging) return undefined;

    /**
     * Actualiza la posicion del elemento durante el drag.
     *
     * @param {MouseEvent|TouchEvent} event evento de movimiento.
     * @returns {void} no retorna valor; actualiza estado.
     *
     * Se calcula coordenadas relativas al canvas y
     * actualiza el elemento en memoria para dar feedback inmediato.
     *
     */
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

    /**
     * Persiste la nueva posicion al finalizar el drag.
     *
     * @returns {Promise<void>} no retorna valor; actualiza backend.
     *
     * Se toma el elemento desde elementsRef (evita stale state)
     * y llama al endpoint de actualizacion.
     *
     */
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

  /**
   * Actualiza el formulario de propiedades del elemento.
   *
   * @param {Event} event evento de cambio en input/select.
   * @returns {void} no retorna valor; solo actualiza estado local.
   *
   * Se usa name/value del input para sobrescribir el campo
   * correspondiente del formulario.
   *
   */
  const handleElementFormChange = (event) => {
    const { name, value } = event.target;
    setElementForm((prev) => ({ ...prev, [name]: value }));
  };

  /**
   * Guarda cambios del elemento seleccionado en el backend.
   *
   * @returns {Promise<void>} no retorna valor; recarga elementos.
   * Si falla la validacion o el backend rechaza, muestra el error.
   *
   * Se validan JSONs opcionales, se normalizan numeros y
   * se persiste el elemento via servicio de actualizacion.
   *
   */
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

  /**
   * Elimina el elemento seleccionado del diagrama.
   *
   * @returns {Promise<void>} no retorna valor; actualiza estado local.
   * Si el backend falla, actualiza el mensaje de error.
   *
   * Se confirma con el usuario, llama al endpoint de borrado
   * y recarga el listado para mantener consistencia.
   *
   */
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

  /**
   * Alterna un elemento dentro de la seleccion para crear conexiones.
   *
   * @param {number} id_elemento id del elemento a seleccionar/deseleccionar.
   * @returns {void} no retorna valor; actualiza seleccion en memoria.
   *
   * Se mantiene un array de max 2 elementos y alterna el id
   * para facilitar la creacion de una conexion.
   *
   */
  const toggleConnectionSelection = (id_elemento) => {
    setSelectedForConnection((prev) => {
      if (prev.includes(id_elemento)) return prev.filter((id) => id !== id_elemento);
      if (prev.length >= 2) return prev;
      return [...prev, id_elemento];
    });
  };

  /**
   * Crea una nueva conexion entre dos elementos seleccionados.
   *
   * @returns {Promise<void>} no retorna valor; limpia seleccion.
   * Si falta seleccion o el backend falla, muestra el error.
   *
   * Se valida que haya 2 elementos seleccionados, se prepara
   * el payload con etiqueta opcional y persiste via backend.
   *
   */
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

  /**
   * Selecciona una conexion para editarla.
   *
   * @param {object} conexion conexion seleccionada.
   * @returns {void} no retorna valor; actualiza estado de seleccion.
   *
   * Se guarda el id de la conexion para cargar el formulario.
   *
   */
  const handleConnectionSelect = (conexion) => {
    setSelectedConnectionId(conexion.id_conexion);
  };

  /**
   * Actualiza el formulario de propiedades de conexion.
   *
   * @param {Event} event evento de cambio en input/select.
   * @returns {void} no retorna valor; solo actualiza estado local.
   *
   * Se usa name/value para actualizar el objeto del formulario.
   *
   */
  const handleConnectionFormChange = (event) => {
    const { name, value } = event.target;
    setConnectionForm((prev) => ({ ...prev, [name]: value }));
  };

  /**
   * Guarda cambios de la conexion seleccionada.
   *
   * @returns {Promise<void>} no retorna valor; recarga conexiones.
   * Si falla la validacion o el backend rechaza, muestra el error.
   *
   * Se validan JSONs opcionales, se normalizan ids y se persiste la conexion via API.
   *
   */
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

  /**
   * Elimina una conexion por id.
   *
   * @param {number} id_conexion id de la conexion a eliminar.
   * @returns {Promise<void>} no retorna valor; actualiza listado.
   * Si el backend falla, actualiza el mensaje de error.
   *
   * Se confirma con el usuario, llama al endpoint de borrado
   * y limpia la seleccion si corresponde.
   *
   */
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

  /**
   * Renderiza el contenido del sidebar izquierdo.
   *
   * @returns {JSX.Element} herramientas y creacion de conexiones.
   *
   * Se muestra la paleta de elementos y la UI para crear
   * relaciones, reutilizando el estado de seleccion.
   *
   */
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

        {/* Herramienta para crear conexiones entre elementos */}
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

  /**
   * Renderiza el contenido del sidebar derecho.
   *
   * @returns {JSX.Element} propiedades del elemento/conexion y lista de capas.
   *
   * Se alterna entre paneles de propiedades segun la seleccion
   * y muestra un listado de capas para navegar/seleccionar elementos.
   *
   */
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
            {LeftSidebarContent()}
          </div>

          {/* Mobile Left Drawer */}
          <div className={`mobile-drawer mobile-drawer-left d-lg-none ${showMobileTools ? 'show' : ''}`}>
            <div className="d-flex flex-column h-100">
              <div className="p-2 border-bottom border-dark-700 d-flex justify-content-between align-items-center">
                <span className="fw-bold text-sm px-2">Herramientas</span>
                <button className="btn btn-sm text-secondary" onClick={() => setShowMobileTools(false)}>✕</button>
              </div>
              {LeftSidebarContent()}
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
            {RightSidebarContent()}
          </div>

          {/* Mobile Right Drawer */}
          <div className={`mobile-drawer mobile-drawer-right d-lg-none ${showMobileProps ? 'show' : ''}`}>
            <div className="d-flex flex-column h-100">
              <div className="p-2 border-bottom border-dark-700 d-flex justify-content-between align-items-center">
                <span className="fw-bold text-sm px-2">Propiedades</span>
                <button className="btn btn-sm text-secondary" onClick={() => setShowMobileProps(false)}>✕</button>
              </div>
              {RightSidebarContent()}
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
