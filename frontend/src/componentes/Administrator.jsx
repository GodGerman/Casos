import React from "react";
import { Button, Container, Table} from "react-bootstrap";
import { Link } from "react-router-dom";

import axios from "axios";
import "../../css/Administrador.css";

class Administrator extends React.Component 
{

    state = {
        data: [],
        showAlert: false,
        alertText: ""
    }

    
    render() {
        const {showAlert, alertText } = this.state;        
        return (
            <Container className="MarginContainer" >
                <h1 className="AlignCenter" > CREAR, ALTAS, BAJAS Y CAMBIOS </h1>
                <hr style={{ width: "80%" }} />
                {
                    showAlert ?
                        <Alert variant="danger">
                            {alertText}
                        </Alert>
                        : null
                }                
                <Button variant="info" style={{ margin: "12px" }}>
                    <Link to="/formulario" className="CustomLink">NUEVA PREGUNTA</Link>
                </Button>
                <Table striped bordered >
                    <thead>
                        <tr>
                            <th>Pregunta</th>
                            <th>Acciones</th>
                        </tr>
                    </thead>
                    <tbody>

                    </tbody>
                </Table>

            </Container>
        )
    }

}

export default Administrator;