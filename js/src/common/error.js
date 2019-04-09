//@flow
import type {SyntaxError} from './types';
import type {MarkerType} from './editor/types';


export function getMarkers(errors: $ReadOnlyArray<SyntaxError>): $ReadOnlyArray<MarkerType> {
    return errors.map((error: SyntaxError): MarkerType => {
        if (error.startLine) {
            return {
                startRow: error.startLine - 1,
                endRow: error.endLine - 1,
                startCol: error.startColumn - 1,
                endCol: error.endColumn - 1,
                severity: error.type || 'error',
                message: error.message
            };
        } else {
            return {
                startRow: 0,
                endRow: 0,
                startCol: 0,
                endCol: 0,
                severity: error.type || 'error',
                message: error.message
            };
        }
    });
}
