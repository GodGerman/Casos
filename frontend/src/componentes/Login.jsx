import React from "react";
import {Navigate} from "react-router-dom"

import "../../css/index.css";

class Login extends React.Component {
  constructor()
  {
    super();
    this.state = {condition: false,tipousuario:''}
 }

  validar=(usuario,password) =>{
    fetch('Login?User='+usuario+'&password='+password+'').then(response => response.json()).then(usuario =>{
      if(usuario.status=="yes"){             
        if(usuario.tipo=="administrador"){
          alert("USUARIO VALIDO");
          this.setState({ condition: true,tipousuario:'administrador'});          
        }          
      }          
      else{
        alert("USUARIO NO VALIDO");
        this.setState({ condition: false,tipousuario:'' });
        document.getElementById("User").value = "";
        document.getElementById("password").value = "";                                        
      }
    })
     
  }
    
  render() {
    const styles = {
      padding : '5px'
    }

    const { condition,tipousuario } = this.state;

    if (condition && tipousuario=="administrador"){
      return <Navigate to='/administrator' />;
    }

    return(  
      <div className = "center-container" style={styles} id="equis">
               
        <div class="form-group">
          <label class="form-label" for="User">Usuario</label>
          <input placeholder="Ingrese el usuario" type="text" id="User" class="form-control" />
        </div>

        <div class="form-group"><label class="form-label" for="password">Password</label>
          <input placeholder="Ingrese su contraseña" type="password" id="password" class="form-control" />
        </div>

        <button className="btn btn-primary mt-3 login-submit" onClick={() => this.validar(document.getElementById("User").value,document.getElementById("password").value)}>
          Submit
        </button>

      </div>)    
  }
}
export default Login; 